package org.bongiorno.edi.reader.loops;

import org.bongiorno.edi.reader.tokenizer.CompositeElement;
import org.bongiorno.edi.reader.tokenizer.Element;
import org.bongiorno.edi.reader.tokenizer.RepeatingElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "startSegment")
public class StartSegment {

    @XmlAttribute(name = "id", required = true)
    protected String id;

    @XmlElement(name = "qualifier", required = false)
    protected ArrayList<Qualifier> qualifiers;

    private static final Logger log = LoggerFactory.getLogger(StartSegment.class);

    public boolean matches(List<Element> segment){
        boolean soFarSoGood = segment.get(0).toString().equals(id);

        if(qualifiers != null) {
            for (Iterator<Qualifier> i = qualifiers.iterator(); soFarSoGood && i.hasNext(); ) {
                Qualifier q = i.next();
                int position = q.getPosition();
                if(position >= segment.size()){
                    soFarSoGood = false;
                }else{
                    Element element = segment.get(position);
                    if(element instanceof CompositeElement || element instanceof RepeatingElement){
                        log.info("Composite or repeating elements don't really work well as qualifiers. (segment {} position {})", id, position);
                        log.debug("Feel free to make a feature request, submit a patch, or just cross your fingers and check something in.");
                    }
                    if(!q.getValues().contains(element.toString())){
                        soFarSoGood = false;
                    }
                }
            }
        }
        return soFarSoGood;
    }

}
