package org.bongiorno.edi.factory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.Map;

/**
 * This class may be used at a later point as part of the JAXB bootstraping method. For now, leave it
 */
public class EdiJaxbContextFactory{



    public static JAXBContext createContext(Class ... classToBeBound) throws JAXBException {
        return new EdiJaxbContext(classToBeBound);
    }

    public static JAXBContext createContext(Class[] classesToBeBound, Map<String, String> properties) throws JAXBException {

        return new EdiJaxbContext(properties,classesToBeBound);
    }

    public static JAXBContext createContext(String contextPath, ClassLoader classLoader) throws JAXBException {
        return JAXBContext.newInstance(contextPath, classLoader);
    }

    public static JAXBContext createContext(String contextPath, ClassLoader classLoader, Map<String,?> properties) throws JAXBException {
        return JAXBContext.newInstance(contextPath, classLoader, properties);
    }

}
