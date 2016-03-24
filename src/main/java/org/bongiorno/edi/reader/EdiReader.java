package org.bongiorno.edi.reader;

import org.bongiorno.edi.reader.loops.EdiStructure;
import org.bongiorno.edi.reader.loops.Loop;
import org.bongiorno.edi.reader.tokenizer.CompositeElement;
import org.bongiorno.edi.reader.tokenizer.EDITokenizer;
import org.bongiorno.edi.reader.tokenizer.Element;
import org.bongiorno.edi.reader.tokenizer.RepeatingElement;
import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class EdiReader extends AbstractXMLReader{

    private boolean includeEmptyElements = true;

    private String rootElement;

    private Deque<String> elementStack = new LinkedList<>();

    private Deque<Loop> loopStack = new LinkedList<>();

    private Map<String, BiConsumer<List<Element>, EDITokenizer>> segmentHandlers;

    private Map<String,Set<Integer>> attDefs;

    public EdiReader(String rootElement, EdiStructure structure){
        loopStack.push(structure.getRootLoop());
        this.rootElement = rootElement;
        this.attDefs = structure.getAttributeDefinitions().stream().collect(Collectors.toMap(a -> a.name,a -> a.position));

        BiConsumer<List<Element>, EDITokenizer> startElement = (segment, tokenizer) -> {
            String segId = segment.get(0).toString();

            Set<Integer> attribPositions = this.attDefs.getOrDefault(segId, Collections.emptySet());
            EdiAttributes attributes = new EdiAttributes();
            for (Integer position : attribPositions) {
                attributes.add(String.format("%s%02d",segId,position),segment.get(position).toString());
            }

            startElement(segId,attributes);
        };

        BiConsumer<List<Element>, EDITokenizer> startIsaElement = (segment, tokenizer) -> {
            String segId = segment.get(0).toString();
            startElement(segId, new EdiAttributes("segmentDelimiter", tokenizer.getSegmentDelimiter(),
                    "elementDelimiter", tokenizer.getElementDelimiter().toString(),
                    "compositeDelimiter", tokenizer.getCompositeDelimiter().toString(),
                    "repetitionSeparator", tokenizer.getRepetitionSeparator().toString()));
        };

        BiConsumer<List<Element>, EDITokenizer> createSubElements = (segment, tokenizer) -> {
            String segId = segment.get(0).toString();
            Set<Integer> attribPositions = this.attDefs.getOrDefault(segId, Collections.emptySet());
            try {
                for (int i = 1; i < segment.size(); ++i) {
                    if(!attribPositions.contains(i)) {
                        String name = String.format("%s%02d", segId, i);
                        addElement(name, segment.get(i));
                    }
                }
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
        };

        BiConsumer<List<Element>, EDITokenizer> popToRootLoop = (x,y) -> {
            while (loopStack.size() > 1){
                endElement();
                loopStack.pop();
            }
        };

        BiConsumer<List<Element>, EDITokenizer> endElement = (x,y) -> endElement();

        BiConsumer<List<Element>, EDITokenizer> doTransition = (segment,x) -> transitionLevel(segment);

        BiConsumer<List<Element>, EDITokenizer> defaultCase = doTransition.andThen(startElement).andThen(createSubElements).andThen(endElement);

        segmentHandlers = LazyMap.lazyMap(new HashMap<>(), x -> defaultCase);

        String endOfLoops = structure.getLoopsEnd();
        if(StringUtils.isNotEmpty(endOfLoops)){
            segmentHandlers.put(endOfLoops, popToRootLoop.andThen(defaultCase));
        }
        segmentHandlers.put("ISA", startIsaElement.andThen(createSubElements));
        segmentHandlers.put("GS", startElement.andThen(createSubElements));
        segmentHandlers.put("ST", startElement.andThen(createSubElements));
        segmentHandlers.put("SE", popToRootLoop.andThen(endElement));
        segmentHandlers.put("GE", endElement);
        segmentHandlers.put("IEA", endElement);
    }

    @Override
    public void parse(Reader reader) throws IOException, SAXException {
        EDITokenizer tokenizer = new EDITokenizer(reader);
        start();
        for (List<Element> segment : tokenizer) {
            String segId = segment.get(0).toString();
            segmentHandlers.get(segId).accept(segment, tokenizer);
        }
        end();
        reader.close();
    }

    private void start() throws SAXException {
        contentHandler.startDocument();
        startElement(rootElement);
    }

    private void end() throws SAXException {
        while(!elementStack.isEmpty()) {
            endElement();
        }
        contentHandler.endDocument();
    }

    private void transitionLevel(List<Element> segment) {
        int levelsToPop = 0;
        Iterator<Loop> iterator = loopStack.iterator();
        Loop nextLoop = iterator.next().getTransition(segment);
        while(nextLoop == null && iterator.hasNext()){
            ++levelsToPop;
            nextLoop = iterator.next().getTransition(segment);
        }

        if(nextLoop != null){
            for(int i = 0; i < levelsToPop; ++i){
                endElement();
                loopStack.pop();
            }
            String loopName = nextLoop.getName();
            if(loopName != null){
                startElement(loopName);
            }
            loopStack.push(nextLoop);
        }
    }

    private void startElement(String elementName) {
        this.startElement(elementName, new AttributesImpl());
    }

    private void startElement(String elementName, Attributes attributes) {
        elementName = elementName.intern();
        elementStack.push(elementName);
        try {
            contentHandler.startElement("", elementName, elementName, attributes);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private void endElement() {
        String name = elementStack.pop();
        try {
            contentHandler.endElement("", name, name);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private void characters(String string) throws SAXException {
        char[] chars = string.toCharArray();
        contentHandler.characters(chars, 0, chars.length);
    }

    private void addElement(String elementName, Element content) throws SAXException {
        if(content instanceof RepeatingElement){
            addElement(elementName, (RepeatingElement) content);
        }else if(content instanceof CompositeElement){
            addElement(elementName, (CompositeElement) content);
        }else{
            this.addElement(elementName, content.toString());            
        }
    }

    private void addElement(String elementName, CompositeElement content) throws SAXException {
        Set<Integer> attribPositions = this.attDefs.getOrDefault(elementName, Collections.emptySet());
        EdiAttributes attributes = new EdiAttributes();
        for (Integer position : attribPositions) {
            attributes.add(String.format("C%02d",position), content.getPart(position-1));
        }

        startElement(elementName,attributes);
        int i = 1;
        for (String s : content.getParts()) {
            int position = i++;
            if(!attribPositions.contains(position)) {
                String name = String.format("C%02d", position);
                this.addElement(name, s);
            }
        }
        endElement();
    }

    private void addElement(String elementName, RepeatingElement content) throws SAXException {
        for (Element element : content.getElements()) {
            addElement(elementName, element);
        }
    }

    private void addElement(String elementName, String content) throws SAXException {
        this.addElement(elementName, new AttributesImpl(), content);
    }

    private void addElement(String elementName, Attributes attributes) throws SAXException {
        startElement(elementName, attributes);
        endElement();
    }

    private void addElement(String elementName, Attributes attributes, String content) throws SAXException {
        if(includeEmptyElements || StringUtils.isNotEmpty(content) || attributes.getLength() > 0) {
            startElement(elementName, attributes);
            characters(content);
            endElement();
        }
    }
}
