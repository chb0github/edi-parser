package org.bongiorno.edi.writer;

import org.bongiorno.edi.reader.EdiAttributes;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import sun.misc.IOUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringBufferInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EdiWriterTest {
    @Test
    public void testISA() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EdiWriter contentHandler = new EdiWriter(baos);

        contentHandler.startDocument();
        contentHandler.startElement("", "edi835", "edi835", new AttributesImpl());

        contentHandler.startElement("", "ISA", "ISA", new EdiAttributes("segmentDelimiter", "\r\n", "elementDelimiter", "*", "compositeDelimiter", ":",
                "repetitionSeparator", "^"));
        contentHandler.startElement("", "ISA01", "ISA01", new AttributesImpl());
        doChars(contentHandler, "00");
        contentHandler.endElement("", "ISA01", "ISA01");
        contentHandler.startElement("", "ISA02", "ISA02", new AttributesImpl());
        doChars(contentHandler, "Authorizat");
        contentHandler.endElement("", "ISA02", "ISA02");
        contentHandler.startElement("", "ISA03", "ISA03", new AttributesImpl());
        doChars(contentHandler, "00");
        contentHandler.endElement("", "ISA03", "ISA03");
        contentHandler.startElement("", "ISA04", "ISA04", new AttributesImpl());
        doChars(contentHandler, "Security I");
        contentHandler.endElement("", "ISA04", "ISA04");
        contentHandler.startElement("", "ISA05", "ISA05", new AttributesImpl());
        doChars(contentHandler, "ZZ");
        contentHandler.endElement("", "ISA05", "ISA05");
        contentHandler.startElement("", "ISA06", "ISA06", new AttributesImpl());
        doChars(contentHandler, "Interchange Sen");
        contentHandler.endElement("", "ISA06", "ISA06");
        contentHandler.startElement("", "ISA07", "ISA07", new AttributesImpl());
        doChars(contentHandler, "ZZ");
        contentHandler.endElement("", "ISA07", "ISA07");
        contentHandler.startElement("", "ISA08", "ISA08", new AttributesImpl());
        doChars(contentHandler, "Interchange Rec");
        contentHandler.endElement("", "ISA08", "ISA08");
        contentHandler.startElement("", "ISA09", "ISA09", new AttributesImpl());
        doChars(contentHandler, "140502");
        contentHandler.endElement("", "ISA09", "ISA09");
        contentHandler.startElement("", "ISA10", "ISA10", new AttributesImpl());
        doChars(contentHandler, "1037");
        contentHandler.endElement("", "ISA10", "ISA10");
        contentHandler.startElement("", "ISA12", "ISA12", new AttributesImpl());
        doChars(contentHandler, "00501");
        contentHandler.endElement("", "ISA12", "ISA12");
        contentHandler.startElement("", "ISA13", "ISA13", new AttributesImpl());
        doChars(contentHandler, "000000001");
        contentHandler.endElement("", "ISA13", "ISA13");
        contentHandler.startElement("", "ISA14", "ISA14", new AttributesImpl());
        doChars(contentHandler, "0");
        contentHandler.endElement("", "ISA14", "ISA14");
        contentHandler.startElement("", "ISA15", "ISA15", new AttributesImpl());
        doChars(contentHandler, "P");
        contentHandler.endElement("", "ISA15", "ISA15");

        contentHandler.startElement("", "GS", "GS", new AttributesImpl());
        contentHandler.startElement("", "GS06", "GS06", new AttributesImpl());
        doChars(contentHandler, "123");
        contentHandler.endElement("", "GS06", "GS06");
        contentHandler.endElement("", "GS", "GS");

        contentHandler.endElement("", "ISA", "ISA");


        contentHandler.endElement("", "edi835", "edi835");
        contentHandler.endDocument();

        assertTrue(baos.toString().startsWith("ISA*00*Authorizat*00*Security I*ZZ*Interchange Sen*ZZ*Interchange Rec*140502*1037*^*00501*000000001*0*P*:\r\n"));
        assertTrue(baos.toString().endsWith("\r\nIEA*1*000000001"));
    }

    @Test
    public void testGsGe() throws Exception {
        String xml = "<edi835>" +
                "<ISA segmentDelimiter=\"&#xD;&#xA;\" elementDelimiter=\"*\" compositeDelimiter=\"\\\" repetitionSeparator=\"*\">" +
                    "<ISA01>00</ISA01>" +
                    "<ISA02>Authorizat</ISA02>" +
                    "<ISA03>00</ISA03>" +
                    "<ISA04>Security I</ISA04>" +
                    "<ISA05>ZZ</ISA05>" +
                    "<ISA06>Interchange Sen</ISA06>" +
                    "<ISA07>ZZ</ISA07>" +
                    "<ISA08>Interchange Rec</ISA08>" +
                    "<ISA09>140502</ISA09>" +
                    "<ISA10>1037</ISA10>" +
                    "<ISA12>00501</ISA12>" +
                    "<ISA13>000000001</ISA13>" +
                    "<ISA14>0</ISA14>" +
                    "<ISA15>T</ISA15>" +
                    "<GS>" +
                        "<GS01>HP</GS01>" +
                        "<GS02>Application Sen</GS02>" +
                        "<GS03>Application Rec</GS03>" +
                        "<GS04>20030326</GS04>" +
                        "<GS05>1037</GS05>" +
                        "<GS06>00001</GS06>" +
                        "<GS07>X</GS07>" +
                        "<GS08>005010X221A1</GS08>" +
                        "<ST/>" +
                        "<ST/>" +
                    "</GS>" +
                "</ISA>" +
                "</edi835>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EdiWriter contentHandler = new EdiWriter(out);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        parser.parse(new StringBufferInputStream(xml), contentHandler);

        String result = out.toString();
        assertTrue(result, result.contains("GS*HP*Application Sen*Application Rec*20030326*1037*00001*X*005010X221A1"));
        assertTrue(result, result.contains("GE*2*00001"));
    }


    @Test
    @Ignore
    public void testStSe() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testMultiIsaIea() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testMultiGsGe() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testMultiStSe() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    public void testSegmentWithNoCompositesOrRepetition() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EdiWriter contentHandler = new EdiWriter(baos);
        String xml =
                "<edi834>" +
                "<ISA segmentDelimiter=\"&#xD;&#xA;\" elementDelimiter=\"~\" compositeDelimiter=\"\\\" repetitionSeparator=\"*\">" +
                        "<ISA13>x</ISA13>" +
                        "<GS>" +
                        "<GS06>123</GS06>" +
                        "<ST>" +
                        "<BGN>" +
                        "<BGN01>00</BGN01>" +
                        "<BGN02>Reference Identification</BGN02>" +
                        "<BGN03>20110201</BGN03>" +
                        "<BGN04>12110500</BGN04>" +
                        "<BGN05>01</BGN05>" +
                        "<BGN06>Reference Identification</BGN06>" +
                        "<BGN07/>" +
                        "<BGN08>2</BGN08>" +
                        "</BGN>" +
                        "</ST>" +
                        "</GS>" +
                        "</ISA>" +
                "</edi834>";

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        parser.parse(new StringBufferInputStream(xml), contentHandler);

        String output = baos.toString();
        assertTrue(output, output.contains("\r\nBGN~00~Reference Identification~20110201~12110500~01~Reference Identification~~2\r\n"));
    }

    @Test
    public void testMultipleSegments() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EdiWriter contentHandler = new EdiWriter(baos);
        String xml =
                "<edi834>" +
                "<ISA segmentDelimiter=\"&#xD;&#xA;\" elementDelimiter=\"~\" compositeDelimiter=\"\\\" repetitionSeparator=\"*\">" +
                        "<ISA13>x</ISA13>" +
                        "<GS>" +
                        "<GS06>123</GS06>" +
                        "<ST>" +
                        "<BGN>" +
                            "<BGN01>00</BGN01>" +
                            "<BGN02>Reference Identification</BGN02>" +
                            "<BGN03>20110201</BGN03>" +
                            "<BGN04>12110500</BGN04>" +
                            "<BGN05>01</BGN05>" +
                            "<BGN06>Reference Identification</BGN06>" +
                            "<BGN07/>" +
                            "<BGN08>2</BGN08>" +
                        "</BGN>" +
                        "<REF>" +
                            "<REF01>38</REF01>" +
                            "<REF02>Reference Identification</REF02>" +
                        "</REF>" +
                        "<DTP>" +
                            "<DTP01>007</DTP01>" +
                            "<DTP02>D8</DTP02>" +
                            "<DTP03>20020202</DTP03>" +
                        "</DTP>" +
                        "</ST>" +
                        "</GS>" +
                        "</ISA>" +
                "</edi834>";

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        parser.parse(new StringBufferInputStream(xml), contentHandler);

        String output = baos.toString();
        assertTrue(output, output.contains("\r\nBGN~00~Reference Identification~20110201~12110500~01~Reference Identification~~2\r\n" +
                "REF~38~Reference Identification\r\n" +
                "DTP~007~D8~20020202\r\n"));
    }

    @Test
    public void testHandleOmittedEmptyElements() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EdiWriter contentHandler = new EdiWriter(baos);
        String xml =
                "<edi834>" +
                    "<ISA segmentDelimiter=\"&#xD;&#xA;\" elementDelimiter=\"~\" compositeDelimiter=\"\\\" repetitionSeparator=\"*\">" +
                        "<ISA13>x</ISA13>" +
                        "<GS>" +
                        "<GS06>123</GS06>" +
                        "<ST>" +
                        "<BGN>" +
                            "<BGN01>00</BGN01>" +
                            "<BGN02>Reference Identification</BGN02>" +
                            "<BGN03>20110201</BGN03>" +
                            "<BGN04>12110500</BGN04>" +
                            "<BGN05>01</BGN05>" +
                            "<BGN06>Reference Identification</BGN06>" +
                            "<BGN08>2</BGN08>" +
                        "</BGN>" +
                        "</ST>" +
                        "</GS>" +
                    "</ISA>" +
                "</edi834>";

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        parser.parse(new StringBufferInputStream(xml), contentHandler);

        String output = baos.toString();
        assertTrue(output, output.contains("\r\nBGN~00~Reference Identification~20110201~12110500~01~Reference Identification~~2\r\n"));
    }

    @Test
    @Ignore
    public void testLoops() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testComposite() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testCompositeWithOmittedEmptyComponents() throws Exception {
        Assert.fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testRepetition() throws Exception {
        Assert.fail("Not yet implemented");
    }

    private void doChars(ContentHandler handler, String str) throws SAXException {
        char[] chars = str.toCharArray();
        handler.characters(chars, 0, chars.length);
    }

    @Test
    @Ignore
    public void testIntegration() throws Exception {

        // run this against some test object not the reference ones
        JAXBContext jc = JAXBContext.newInstance(Object.class);
//        JAXBContext jc = JAXBContext.newInstance(Document837.class);


        InputStream resourceAsStream = this.getClass().getResourceAsStream("/834/834.dat");
        byte[] input = IOUtils.readFully(resourceAsStream, Integer.MAX_VALUE, false);
        SAXSource source = new SAXSource(null, new InputSource(new ByteArrayInputStream(input)));

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        Object result = unmarshaller.unmarshal(source);

        Marshaller marshaller = jc.createMarshaller();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EdiWriter contentHandler = new EdiWriter(baos);

        marshaller.marshal(result, contentHandler);

        assertEquals(new String(input), baos.toString());
    }
}