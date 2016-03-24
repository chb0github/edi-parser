package org.bongiorno.edi.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;

public class EdiWriter extends DefaultHandler {


    private Charset encoding = Charset.defaultCharset();

    private byte[] segmentDelimiter;

    private byte[] elementDelimiter;

    private byte[] compositeDelimiter;

    private byte[] repetitionSeparator;

    private OutputStream out;

    private ParseState state = new StartState();



    public EdiWriter(OutputStream out) {
        this.out = out;

    }

    public EdiWriter(File out) throws FileNotFoundException {
        this.out = new BufferedOutputStream(new FileOutputStream(out));
    }

    public EdiWriter(String fileName) throws FileNotFoundException {
        this.out = new BufferedOutputStream(new FileOutputStream(fileName));
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        try {
            state = state.startElement(qName, atts);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            state = state.endElement();
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            state.characters(CharBuffer.wrap(ch, start, length));
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }


    private abstract class ParseState {

        protected ParseState parent;

        protected ParseState(ParseState parent) {
            this.parent = parent;
        }

        public void characters(CharBuffer buffer) throws IOException {
            emitChars(buffer);
        }
        
        public abstract ParseState startElement(String qName, Attributes atts) throws SAXException, IOException;

        public ParseState endElement() throws IOException {
            return parent;
        }
    }

    private class StartState extends ParseState {

        private StartState() {
            super(null);
            super.parent = this;
        }

        @Override
        public ParseState startElement(String qName, Attributes atts) throws IOException {
            if (qName.equals("ISA")) {
                segmentDelimiter = atts.getValue("segmentDelimiter").getBytes(encoding);
                elementDelimiter = atts.getValue("elementDelimiter").getBytes(encoding);
                compositeDelimiter = atts.getValue("compositeDelimiter").getBytes(encoding);
                repetitionSeparator = atts.getValue("repetitionSeparator").getBytes(encoding);
                emitChars(qName);
                return new ISAState(this);
            }
            return this;
        }
    }

    private class SegmentState extends ParseState {

        private String segmentId;
        protected int elementCount = 0;

        private SegmentState(String segmentId, ParseState parent) {
            super(parent);
            this.segmentId = segmentId;
        }

        @Override
        public ParseState startElement(String qName, Attributes atts) throws IOException, SAXException{
            if (qName.startsWith(segmentId)) {
                int elementNumber = Integer.parseInt(qName.substring(segmentId.length()));
                if(elementNumber <= elementCount){
                    throw new SAXException(String.format("Elements out of order: %s%02d encountered after %s%02d", segmentId, elementNumber, segmentId, elementCount));
                }
                while(elementCount < elementNumber){
                    out.write(elementDelimiter);
                    ++elementCount;
                }
                return new ElementState(qName, this);
            } else {
                return nextSegment(qName);
            }
        }

        protected SegmentState nextSegment(String qName) throws IOException {
            out.write(segmentDelimiter);
            emitChars(qName);
            return new SegmentState(qName, this);
        }
    }

    private class ISAState extends SegmentState {

        private ElementState controlNumber;
        private int groupCount;

        public ISAState(StartState parent) {
            super("ISA", parent);
        }

        @Override
        public ParseState startElement(String qName, Attributes atts) throws IOException, SAXException {
            if("ISA12".equals(qName)){
                ++elementCount;
                out.write(elementDelimiter);
                out.write(repetitionSeparator);
            }
            ParseState result = super.startElement(qName, atts);
            if("ISA13".equals(qName)){
                controlNumber = (ElementState) result;
            }
            return result;
        }

        @Override
        protected SegmentState nextSegment(String qName) throws IOException {
            out.write(elementDelimiter);
            out.write(compositeDelimiter);

            ++groupCount;
            out.write(segmentDelimiter);
            emitChars(qName);
            return new GroupState(this);
        }

        @Override
        public ParseState endElement() throws IOException {
            out.write(segmentDelimiter);
            emitChars("IEA");
            out.write(elementDelimiter);
            emitChars(Integer.toString(groupCount));
            out.write(elementDelimiter);
            emitChars(controlNumber.getContents());
            return super.endElement();
        }
    }

    private class GroupState extends SegmentState{

        private ElementState controlNumber;
        private int stCount;

        private GroupState(ParseState parent) {
            super("GS", parent);
        }

        @Override
        public ParseState startElement(String qName, Attributes atts) throws IOException, SAXException {
            ParseState result = super.startElement(qName, atts);
            if("GS06".equals(qName)){
                controlNumber = (ElementState) result;
            }
            return result;
        }

        @Override
        protected SegmentState nextSegment(String qName) throws IOException {
            ++stCount;
            return new TransactionState(this);
        }

        @Override
        public ParseState endElement() throws IOException {
            out.write(segmentDelimiter);
            emitChars("GE");
            out.write(elementDelimiter);
            emitChars(Integer.toString(stCount));
            out.write(elementDelimiter);
            emitChars(controlNumber.getContents());
            return super.endElement();
        }
    }

    public class TransactionState extends SegmentState {
        public TransactionState(ParseState parent) {
            super("ST", parent);
        }
    }

    private class ElementState extends ParseState {

        private CharBuffer contents;
        private final String elementName;

        public ElementState(String elementName, ParseState parent) {
            super(parent);
            this.elementName = elementName;
        }

        @Override
        public ParseState startElement(String qName, Attributes atts) {
            // if this gets called then you're in a nested element.
            // determine if it's composite, another element, loop, etc.
            if (qName.matches("Loop.*"))
                return new LoopState(this);

            if (qName.matches("C\\d{2}"))
                return new CompositeState(this);

            return new ElementState(elementName, this);
        }

        @Override
        public void characters(CharBuffer buffer) throws IOException {
            contents = buffer.duplicate();
            super.characters(buffer);
        }

        public CharBuffer getContents() {
            return contents;
        }
    }

    private class LoopState extends ParseState {
        private LoopState(ParseState parent) {
            super(parent);
        }

        @Override
        public ParseState startElement(String qName, Attributes atts) throws SAXException {
            if (qName.matches("Loop.*"))
                return new LoopState(this);

            if (!qName.matches("C\\d{2}")) // you can't have composite in a loop so this must be an element. Hmm, we need to capture the name.
                return new ElementState("", this);

            throw new SAXException("Illegal Element " + qName);
        }
    }

    private class CompositeState extends ParseState {
        private CompositeState(ParseState parent) {
            super(parent);
        }

        @Override
        public ParseState startElement(String qName, Attributes atts) {
            return null;
        }

        @Override
        public void characters(CharBuffer buffer) throws IOException {

        }
    }
//        LOOP {
//            @Override
//            public ParseState startElement(EdiWriter writer, String qName, Attributes atts) {
//                // may transition to ELEMENT but should put nothing on the stack
//                return ELEMENT;
//            }
//        },
//        COMPOSITE {
//            @Override
//            public ParseState startElement(EdiWriter writer, String qName, Attributes atts) throws IOException {
//                // may write to output but may only transition to element
//                if (!qName.matches("C\\d{2}")) {
//                    return ELEMENT;
//                }
//                writer.emitComposite();
//                writer.emitSegmentDelimiter(qName);
//                return COMPOSITE;
//
//            }
//        },
//        DONE {
//            @Override
//            public ParseState startElement(EdiWriter writer, String qName, Attributes atts) {
//                // MAY ONLY go to start to process more ISA
//                return START;
//            }
//        };

    private void emitChars(String chars) throws IOException {
        this.emitChars(CharBuffer.wrap(chars));
    }

    private void emitChars(CharBuffer chars) throws IOException {
        ByteBuffer bytes = encoding.encode(chars);
        out.write(bytes.array(), bytes.arrayOffset(), bytes.remaining());
    }
}
