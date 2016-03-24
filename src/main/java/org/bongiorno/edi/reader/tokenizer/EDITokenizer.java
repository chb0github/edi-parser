package org.bongiorno.edi.reader.tokenizer;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EDITokenizer implements Iterable<List<Element>>, Iterator<List<Element>> {

    private StreamTokenizer tokenizer;

    private String segmentDelimiter;
    private Character elementDelimiter;
    private Pattern elementDelimiterRegex;
    private Character compositeDelimiter;
    private Character repetitionSeparator;
    private Function<String, Element> elementTransformer = string -> Element.create(string, compositeDelimiter, repetitionSeparator);

    public EDITokenizer(Reader in) throws IOException {
        this(new BufferedReader(in, 4096));
    }

    public EDITokenizer(InputStream in, Charset charSet) throws IOException {
        this(new BufferedReader(new InputStreamReader(in, charSet), 4096));
    }

    public EDITokenizer(InputStream in, String charSet) throws IOException {
        this(new BufferedReader(new InputStreamReader(in, Charset.forName(charSet)), 4096));
    }


    public EDITokenizer(BufferedReader reader) throws IOException {
        reader.mark(128);
        if(reader.read() == '\uFEFF') {
            reader.mark(128);
        }else{
            reader.reset();
        }

        char[] buff = new char[108];
        int readResult = reader.read(buff);
        if(readResult == -1){
            throw new IOException("EOF before any data");
        }

        elementDelimiter = buff[3];
        repetitionSeparator = buff[82];
        compositeDelimiter = buff[104];
        segmentDelimiter = new String(buff, 105, 3);
        segmentDelimiter = segmentDelimiter.replaceFirst("^(.?\\r?\\n?).?.?", "$1");
        this.elementDelimiterRegex = Pattern.compile(Pattern.quote(String.valueOf(elementDelimiter)));

        validateFirstSegment(buff, elementDelimiter);

        tokenizer = new StreamTokenizer(reader);
        tokenizer.resetSyntax();
        // Everything is part of a token except the segment delimiter.
        tokenizer.wordChars(Character.MIN_VALUE, Character.MAX_VALUE);
        for(int i = 0; i < segmentDelimiter.length(); ++i){
            char c = segmentDelimiter.charAt(i);
            tokenizer.whitespaceChars(c, c);
        }

        reader.reset();
    }

    public Character getRepetitionSeparator() {
        return repetitionSeparator;
    }

    public Character getCompositeDelimiter() {
        return compositeDelimiter;
    }

    public Character getElementDelimiter() {
        return elementDelimiter;
    }

    public String getSegmentDelimiter() {
        return segmentDelimiter;
    }

    private void validateFirstSegment(char[] buff, char elementDelimiter) throws IOException {
        if (!"ISA".equalsIgnoreCase(new String(buff, 0, 3))) {
            throw new IOException("Data does not start with an ISA segment. It starts: '" + new String(buff)+"'");
        }
        int elementCount = 1;
        for (char c : buff) {
            if( c == elementDelimiter){
                ++elementCount;
            }
        }
        if(elementCount != 17){
            throw new IOException("Fixed length ISA segment doesn't have the right number of elements. Segment: '" + new String(buff) + "'");
        }
    }

    @Override
    public Iterator<List<Element>> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        try {
            tokenizer.nextToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        tokenizer.pushBack();
        return tokenizer.ttype != StreamTokenizer.TT_EOF;
    }

    @Override
    public List<Element> next() {
        String raw = nextRaw();
        String[] elements = elementDelimiterRegex.split(raw, -1);
        return Arrays.stream(elements).map(elementTransformer).collect(Collectors.toList());
    }

    public String nextRaw() {
        try {
            tokenizer.nextToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
            throw new NoSuchElementException();
        }
        return tokenizer.sval;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Can't modify the underlying data stream");
    }
}
