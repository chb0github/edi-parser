package org.bongiorno.edi.reader.tokenizer;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Predicate;


public enum EDIType {
    /* Justin
   Basically, for:
           837I,P,D, 820, 834 use GS08
           270,271,276,277 use ST01
           278 use ST01, then for determining 278 request vs response use BHT02

   Or some hierarchy of that nature.  I haven’t mapped it out for other transaction types at the moment.
    */
    FORMAT_270("270", new ElementMatcher("ST", 1, "270"), "HL", "TRN"),
    FORMAT_271("271", new ElementMatcher("ST", 1, "271"), "HL", null),
    FORMAT_276("276", new ElementMatcher("ST", 1, "276"), "HL", "TRN"),
    FORMAT_277("277", new ElementMatcher("ST", 1, "277"), "HL", null),
    FORMAT_278_REQ("278Req", new ElementMatcher("ST", 1, "278")
                .and(
                        new ElementMatcher("BHT", 2, "13")
                            .or(
                            new ElementMatcher("BHT", 2, "01"))
                            .or(
                            new ElementMatcher("BHT", 2, "36"))), "HL", "ST"),
    FORMAT_278_RES("278Res", new ElementMatcher("ST", 1, "278").and(new ElementMatcher("BHT", 2, "11")), "HL", null),
    FORMAT_820("820", new ElementMatcher("GS", 8, "005010X218"), "ENT", "ST"),
    FORMAT_834("834", new ElementMatcher("GS", 8, "005010X220"), "INS", "INS"),
    FORMAT_835("835", new ElementMatcher("GS", 8, "005010X221"), "LX", "ST"),
    FORMAT_837D("837d", new ElementMatcher("GS", 8, "005010X224"), "HL", "CLM"),
    FORMAT_837I("837i", new ElementMatcher("GS", 8, "005010X223"), "HL", "CLM"),
    FORMAT_837P("837p", new ElementMatcher("GS", 8, "005010X222"), "HL", "CLM");

    private String simpleName;

    private String headerEndSegment;

    /**
     * The number of times this segment appears == the number of transactions in the file
     */
    private String uniqueTransactionSegment;

    private Predicate<Iterable<List<Element>>> headerMatchPredicate;

    private static final List<EDIType> VALUES = Arrays.asList(EDIType.values());
    private static final Map<String, EDIType> stringLookup;

    static {
        stringLookup = new HashMap<>();
        for (EDIType ediType : EDIType.values()) {
            stringLookup.put(ediType.getSimpleName().toUpperCase(), ediType);
        }
    }

    EDIType(String simpleName, Predicate<Iterable<List<Element>>> headerMatchPredicate, String headerEndSegment, String uniqueTransactionSegment) {
        this.simpleName = simpleName;
        this.headerEndSegment = headerEndSegment;
        this.headerMatchPredicate = headerMatchPredicate;
        this.uniqueTransactionSegment = uniqueTransactionSegment;
    }

    public static EDIType fromString(String name) {
        return stringLookup.get(name.toUpperCase());
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getHeaderEndSegment() {
        return headerEndSegment;
    }

    public String getUniqueTransactionSegment() {
        return uniqueTransactionSegment;
    }

    @Override
    public String toString() {
        return simpleName;
    }

    public static EDIType fromStream(InputStream in, Charset charset) throws IOException {
        final List<List<Element>> headers = new ArrayList<>(3);
        EDITokenizer ediTokenizer = new EDITokenizer(new InputStreamReader(in, charset));
        ediTokenizer.next(); //ISA
        headers.add(ediTokenizer.next()); // GS
        headers.add(ediTokenizer.next()); // ST
        headers.add(ediTokenizer.next()); // BHT
        return VALUES.stream().filter(type ->  type.headerMatchPredicate.test(headers)).findFirst().get();
    }

    private static class ElementMatcher implements Predicate<Iterable<List<Element>>> {

        private String targetSegmentId;
        private int targetIndex;

        private String targetString;

        private ElementMatcher(String targetSegmentId, int targetIndex, String targetString) {
            this.targetSegmentId = targetSegmentId;
            this.targetIndex = targetIndex;
            this.targetString = targetString;
        }

        @Override
        public boolean test(Iterable<List<Element>> segments) {
            List<Element> targetSegment = null;
            for (List<Element> segment : segments) {
                if (targetSegmentId.equals(segment.get(0).toString())) {
                    targetSegment = segment;
                    break;
                }
            }
            if (targetSegment == null) {
                throw new IllegalArgumentException(targetSegmentId + " segment not found");
            }
            if (targetSegment.size() <= targetIndex) {
                throw new IllegalArgumentException(String.format("%s%02d not found (segment too short)", targetSegment, targetIndex));
            }

            Element element = targetSegment.get(targetIndex);
            return element.toString().startsWith(targetString);
        }
    }
}
