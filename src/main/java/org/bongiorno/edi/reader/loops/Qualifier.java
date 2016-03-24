
package org.bongiorno.edi.reader.loops;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "qualifier")
public class Qualifier {

    @XmlAttribute(name = "position", required = true)
    private int position;

    @XmlElement(name = "value", required = true)
    private List<String> values;

    public int getPosition() {
        return position;
    }

    public List<String> getValues() {
        return values;
    }
}
