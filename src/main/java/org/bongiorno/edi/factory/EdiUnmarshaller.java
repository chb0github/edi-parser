package org.bongiorno.edi.factory;

import org.bongiorno.edi.reader.EdiReader;
import org.bongiorno.edi.reader.loops.EdiStructure;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URL;

/**
 * @author chribong
 */
public class EdiUnmarshaller implements Unmarshaller {

    private final Class bindClass;
    private final String rootElementName;
    private Unmarshaller delegate;
    private final EdiStructure structure;
    private final URL loopResource;

    public EdiUnmarshaller(Class bindClass, Unmarshaller delegate) throws JAXBException {
        this(bindClass,findLoopResource(bindClass),delegate);


    }
    public EdiUnmarshaller(Class bindClass, URL loopResource, Unmarshaller delegate) throws JAXBException {


        this.bindClass = bindClass;
        this.delegate = delegate;
        try {
            this.structure = EdiStructure.fromStream(loopResource.openStream());
        } catch (IOException e) {
            throw new JAXBException(e);
        }
        // instead of creating a 'File' that we should need to split the .xml off of anyways, we can just parse it ourselves.
        String[] split = loopResource.getFile().split("/");
        this.rootElementName = split[split.length -1].split("\\.")[0];
        this.loopResource = loopResource;
    }

    private static URL findLoopResource(Class bindClass) {
        String loopFileName = getRootElementName(bindClass);
        String name = String.format("/META-INF/loops/%s.xml", loopFileName);
        URL resource = EdiUnmarshaller.class.getResource(name);

        if(resource == null)
            throw new IllegalArgumentException("Loop resource definition not found. Please put a loop definition in: " + name);
        return resource;
    }

    private static String getRootElementName(Class bindClass) {
        String rootElementName = bindClass.getSimpleName();
        Annotation annotation = bindClass.getAnnotation(XmlRootElement.class);
        if (annotation == null) {
            annotation = bindClass.getAnnotation(XmlType.class);
            if (annotation != null) {
                rootElementName = ((XmlType) annotation).name();
            }
        } else {
            String temp = ((XmlRootElement) bindClass.getAnnotation(XmlRootElement.class)).name();
            // if not set this is what you get and that won't do
            if(!temp.equals("##default"))
                rootElementName = temp;
        }
        return rootElementName;
    }

    public URL getLoopResource() {
        return loopResource;
    }

    public String getRootElementName() {
        return rootElementName;
    }

    @Override
    public <A extends XmlAdapter> A getAdapter(Class<A> type) {
        return delegate.getAdapter(type);
    }

    @Override
    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
        return delegate.getAttachmentUnmarshaller();
    }

    @Override
    public ValidationEventHandler getEventHandler() throws JAXBException {
        return delegate.getEventHandler();
    }

    @Override
    public Listener getListener() {
        return delegate.getListener();
    }

    @Override
    public Object getProperty(String name) throws PropertyException {
        return delegate.getProperty(name);
    }

    @Override
    public Schema getSchema() {
        return delegate.getSchema();
    }

    @Override
    public UnmarshallerHandler getUnmarshallerHandler() {
        return delegate.getUnmarshallerHandler();
    }

    @Override
    public boolean isValidating() throws JAXBException {
        return delegate.isValidating();
    }

    @Override
    public void setAdapter(XmlAdapter adapter) {
        delegate.setAdapter(adapter);
    }

    @Override
    public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        delegate.setAdapter(type, adapter);
    }

    @Override
    public void setAttachmentUnmarshaller(AttachmentUnmarshaller au) {
        delegate.setAttachmentUnmarshaller(au);
    }

    @Override
    public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
        delegate.setEventHandler(handler);
    }

    @Override
    public void setListener(Listener listener) {
        delegate.setListener(listener);
    }

    @Override
    public void setProperty(String name, Object value) throws PropertyException {
        delegate.setProperty(name, value);
    }

    @Override
    public void setSchema(Schema schema) {
        delegate.setSchema(schema);
    }

    @Override
    public void setValidating(boolean validating) throws JAXBException {
        delegate.setValidating(validating);
    }


    @Override
    public Object unmarshal(Node node) throws JAXBException {
        return delegate.unmarshal(node);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType) throws JAXBException {
        return delegate.unmarshal(node, declaredType);
    }

    @Override
    public Object unmarshal(InputStream is) throws JAXBException {
        return unmarshal(new InputSource(is));
    }

    @Override
    public Object unmarshal(Reader reader) throws JAXBException {
        return unmarshal(new InputSource(reader));
    }

    @Override
    public Object unmarshal(File f) throws JAXBException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            throw new JAXBException(e);
        }
        return unmarshal(new InputSource(new BufferedInputStream(in)));
    }

    @Override
    public Object unmarshal(URL url) throws JAXBException {
        InputStream byteStream = null;
        try {
            byteStream = url.openStream();
        } catch (IOException e) {
            throw new JAXBException(e);
        }
        return unmarshal(new InputSource(byteStream));
    }

    @Override
    public Object unmarshal(InputSource source) throws JAXBException {


        XMLReader xmlReader = new EdiReader(rootElementName, structure);

        return delegate.unmarshal(new SAXSource(xmlReader, source));
    }

    @Override
    public Object unmarshal(XMLEventReader reader) throws JAXBException {
        return delegate.unmarshal(reader);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(XMLEventReader reader, Class<T> declaredType) throws JAXBException {
        return delegate.unmarshal(reader, declaredType);
    }

    @Override
    public Object unmarshal(XMLStreamReader reader) throws JAXBException {
        return delegate.unmarshal(reader);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(XMLStreamReader reader, Class<T> declaredType) throws JAXBException {
        return delegate.unmarshal(reader, declaredType);
    }


    @Override
    public Object unmarshal(Source source) throws JAXBException {
        return delegate.unmarshal(source);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(Source source, Class<T> declaredType) throws JAXBException {
        return delegate.unmarshal(source, declaredType);
    }

}
