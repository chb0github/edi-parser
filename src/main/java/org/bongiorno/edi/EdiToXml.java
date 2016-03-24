package org.bongiorno.edi;

import org.bongiorno.edi.reader.EdiReader;
import org.bongiorno.edi.reader.loops.EdiStructure;
import org.bongiorno.edi.reader.tokenizer.EDIType;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

/**
 * @author chribong
 */
public class EdiToXml {

    /*
Help options
 */
    @Option(name = "--help", aliases = {"-?", "-h"}, help = true, usage = "Print this message")
    private boolean help;

    @Option(name = "--out", aliases = "-o", handler = OutputStreamOptionHandler.class, required = false, usage = "Destination file.")
    private OutputStream destination;


    @Option(name = "--in", aliases = "-i", handler = InputStreamOptionHandler.class, usage = "Edi input")
    private BufferedInputStream inEdi;

    @Option(name = "--style", aliases = "-s", usage = "Style Sheet to startElement", handler = InputStreamOptionHandler.class)
    private InputStream styleSheet;

    @Option(name = "--version", aliases = "-v", usage = "output the version of this app", help = true)
    private boolean displayVersion;

    public static void main(String[] args) throws CmdLineException, JAXBException, IOException, TransformerException {

        EdiToXml app = new EdiToXml();
        CmdLineParser cmdLineParser = new CmdLineParser(app);
        cmdLineParser.parseArgument(args);

        if(app.isHelp()){
            cmdLineParser.printUsage(System.out);
            System.exit(0);
        }
        if(app.displayVersion) {
            Properties properties = new Properties();
            properties.load(app.getClass().getResourceAsStream("/app.properties"));

            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                System.out.printf("%s: %s%s",entry.getKey(),entry.getValue(),System.lineSeparator());
            }
            System.exit(0);
        }

        app.run();
    }

    private void run() throws IOException, TransformerException, JAXBException {
        if(destination == null){
            destination = System.out;
        }
        if(inEdi == null){
            inEdi = new BufferedInputStream(System.in);
        }
        if(styleSheet == null){
            styleSheet = this.getClass().getResourceAsStream("/identity.xslt");
        }

        inEdi.mark(128);
        EDIType type = EDIType.fromStream(inEdi, Charset.defaultCharset());
        inEdi.reset();
        InputStream resource;
        String rootName;
        switch (type){
            case FORMAT_837D:
            case FORMAT_837I:
            case FORMAT_837P:
                resource = getClass().getResourceAsStream("/META-INF/loops/edi837.xml");
                rootName = "edi837";
                break;
            case FORMAT_835:
                resource = getClass().getResourceAsStream("/META-INF/loops/edi835.xml");
                rootName = "edi835";
                break;
            case FORMAT_834:
                resource = getClass().getResourceAsStream("/META-INF/loops/edi834.xml");
                rootName = "edi834";
                break;
            default:
                throw new RuntimeException("Unsupported format: " + type);
        }

        EdiStructure structure = EdiStructure.fromStream(resource);
        XMLReader xmlReader = new EdiReader(rootName,structure);

        SAXSource ediSource = new SAXSource(xmlReader, new InputSource(inEdi));
        StreamSource xsltSource = new StreamSource(styleSheet);



        Transformer transformer = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null).newTransformer(xsltSource);
        transformer.transform(ediSource, new StreamResult(destination));
        destination.flush();
        destination.close();
        inEdi.close();
    }

    public boolean isHelp() {
        return help;
    }

    public static class InputStreamOptionHandler extends OneArgumentOptionHandler<BufferedInputStream> {

        public InputStreamOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super BufferedInputStream> setter) {
            super(parser, option, setter);
        }

        @Override
        protected BufferedInputStream parse(String argument) throws NumberFormatException, CmdLineException {
            InputStream result = null;
            try {
            if(argument.equals("-")) {
                result = System.in;
            }
            else {
                result = new FileInputStream(argument);
            }

            } catch (FileNotFoundException e) {
                throw new CmdLineException(owner,e.getMessage());
            }
            return new BufferedInputStream(result);
        }
    }

    public static class OutputStreamOptionHandler extends OneArgumentOptionHandler<OutputStream> {

        public OutputStreamOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super OutputStream> setter) {
            super(parser, option, setter);
        }

        @Override
        protected OutputStream parse(String argument) throws NumberFormatException, CmdLineException {
            OutputStream result;
            try {
                if(argument.equals("-")) {
                    result = System.out;
                }
                else {
                    result = new BufferedOutputStream(new FileOutputStream(argument));
                }
            } catch (FileNotFoundException e) {
                throw new CmdLineException(owner,e.getMessage());
            }
            return result;
        }
    }



}
