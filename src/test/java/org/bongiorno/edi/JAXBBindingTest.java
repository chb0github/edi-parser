package org.bongiorno.edi;

import org.bongiorno.edi.factory.EdiUnmarshaller;
import org.junit.Test;

import javax.xml.bind.annotation.XmlRootElement;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author chribong
 */
public class JAXBBindingTest {

    @Test
    public void testGetsLoopFileFromMETA837() throws Exception {

        //
        EdiUnmarshaller um = new EdiUnmarshaller(Document837.class,null);
        URL result = um.getLoopResource();
        URL expected = this.getClass().getResource("/META-INF/loops/edi837.xml");
        assertEquals(expected,result);

        assertEquals("edi837",um.getRootElementName());

    }

    @Test
    public void testGetsLoopFileAsClassName() throws Exception {

        EdiUnmarshaller um = new EdiUnmarshaller(Foo.class,null);
        URL result = um.getLoopResource();
        URL expected = this.getClass().getResource("/META-INF/loops/Foo.xml");
        assertEquals(expected,result);

        assertEquals(Foo.class.getSimpleName(), um.getRootElementName());

    }

    @XmlRootElement
    private static class Foo {

    }

    @XmlRootElement(name = "edi837")
    private static class Document837 {

    }
}
