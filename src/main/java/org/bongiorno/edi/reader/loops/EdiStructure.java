package org.bongiorno.edi.reader.loops;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "X12-Structure")
public class EdiStructure {


    @XmlAttribute
    private String loopsEnd;

    @XmlElement(name = "Loop", required = true)
    private List<Loop> loops;

    @XmlElement(name = "AttributeDefinition")
    private Set<AttribDef> attributeDefinitions = new HashSet<>();

    public String getLoopsEnd() {
        return loopsEnd;
    }

    public Loop getRootLoop() {
        return loops.size() == 1 ? loops.get(0)
                : loops.stream()
                .filter(l -> "ROOT".equals(l.getId()))
                .findAny().orElseThrow(() -> new RuntimeException("One top-level loop should have id='ROOT'"));
    }

    public Set<AttribDef> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public static EdiStructure fromStream(InputStream input) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(EdiStructure.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        EdiStructure ediStructure = (EdiStructure) unmarshaller.unmarshal(input);

        if(ediStructure.loops == null || ediStructure.loops.isEmpty()){
            throw new RuntimeException("One Loop element is required");
        }
        Map<String, Loop> loopMap = ediStructure.loops.stream().collect(Collectors.toMap(Loop::getId, Function.identity(),
                (x, y) -> {
                    throw new RuntimeException("Multiple top-level loops with the same id: '" + x.getId() + "'");
                }
        ));
        ediStructure.loops.stream().forEach(l -> l.resolveLoopRefs(loopMap));

        return ediStructure;
    }


    public static class AttribDef {
        public String name;
        public Set<Integer> position;
    }
}
