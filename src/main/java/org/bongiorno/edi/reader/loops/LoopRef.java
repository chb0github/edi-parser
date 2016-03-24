
package org.bongiorno.edi.reader.loops;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "LoopRef")
public class LoopRef {

    @XmlAttribute(name = "id")
    protected String id;

    public String getId() {
        return id;
    }
}
