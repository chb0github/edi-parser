
package org.bongiorno.edi.reader.loops;

import org.bongiorno.edi.reader.tokenizer.Element;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Loop")
public class Loop {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "name")
    private String name;

    private StartSegment startSegment;

    @XmlElement(name = "continueSegment")
    private List<String> continueSegments;

    @XmlElement(name = "Loop")
    private List<Loop> loops;

    @XmlElement(name = "LoopRef")
    private List<LoopRef> loopRefs;

    @XmlTransient
    private List<Loop> children;

    public String getId() {
        return id;
    }

    public String getName() {
        return "Loop" + name;
    }

    void resolveLoopRefs(Map<String, Loop> loopMap) {
        if (children != null) {
            throw new IllegalStateException("resolveLoopRefs called multiple times");
        }
        children = new ArrayList<>();
        if (loops != null) {
            loops.stream().forEach(l -> l.resolveLoopRefs(loopMap));
            children.addAll(loops);
        }
        if (loopRefs != null) {
            loopRefs.stream().forEach(ref -> {
                String id = ref.getId();
                Loop referent = loopMap.get(id);
                if (referent == null) {
                    throw new RuntimeException("Bad LoopRef: No top-level loop defined with id='" + id + "'");
                }
                children.add(referent);
            });
        }
    }

    public Loop getTransition(List<Element> segment) {
        Loop result = null;
        String segId = segment.get(0).toString();
        if (continueSegments == null || !continueSegments.contains(segId)) {
            result = children.stream()
                    .filter(c -> c.startSegment.matches(segment))
                    .findFirst()
                    .orElse(null);
        }
        return result;
    }
}
