package org.bongiorno.edi.factory;


import org.w3c.dom.Node;

import javax.xml.bind.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.eclipse.persistence.jaxb.JAXBContextFactory;

public class EdiJaxbContext extends JAXBContext {

    private Class bindClass;


    /**
     * Because the mechanisms for discovering the JAXB parsing factory don't actually implement the interface as per the OSGI model we can either totally replicate what
     * the default java factory does for discover (literally C and P) or we can just hard code it. We are opting for HC
     * because it has the fewest question and we are betting no one will care.
     */
    private JAXBContext delegate;

    /**
     * Only binds to one class at the moment.
     * @param bindClass
     * @throws JAXBException if there is something underneath that doesn't like you
     */
    public EdiJaxbContext(Class... bindClass) throws JAXBException {
        this.bindClass = bindClass[0];
        this.delegate = JAXBContextFactory.createContext(bindClass, new Properties());
    }


    public EdiJaxbContext(Map<String, ?> properties, Class... bindClass) throws JAXBException {
        this.bindClass = bindClass[0];
        this.delegate = JAXBContextFactory.createContext(bindClass,properties);
    }


    @Override
    public Binder<Node> createBinder() {
        return delegate.createBinder();
    }

    @Override
    public <T> Binder<T> createBinder(Class<T> domType) {
        return delegate.createBinder(domType);
    }

    @Override
    public JAXBIntrospector createJAXBIntrospector() {
        return delegate.createJAXBIntrospector();
    }

    @Override
    public Marshaller createMarshaller() throws JAXBException {
        return delegate.createMarshaller();
    }

    @Override
    public Unmarshaller createUnmarshaller() throws JAXBException {
        return new EdiUnmarshaller(this.bindClass, delegate.createUnmarshaller());
    }

    @Override
    public Validator createValidator() throws JAXBException {
        return delegate.createValidator();
    }

    @Override
    public void generateSchema(SchemaOutputResolver outputResolver) throws IOException {
        delegate.generateSchema(outputResolver);
    }

    public static JAXBContext newInstance(Class... classesToBeBound) throws JAXBException {
        return newInstance(classesToBeBound, Collections.emptyMap());
    }

    public static JAXBContext newInstance(Class[] classesToBeBound, Map<String, ?> properties) throws JAXBException {
        //FIXME: Arbitrarily using the first class to determine the loop structure and root element name
        //FIXME: FOR NOW WE ONLY SUPPORT 1 class at a time
        return new EdiJaxbContext(properties,classesToBeBound);
    }

    public static JAXBContext newInstance(String contextPath) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public static JAXBContext newInstance(String contextPath, ClassLoader classLoader) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public static JAXBContext newInstance(String contextPath, ClassLoader classLoader, Map<String, ?> properties) throws JAXBException {
        throw new UnsupportedOperationException();
    }
}
