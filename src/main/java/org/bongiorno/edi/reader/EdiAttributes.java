package org.bongiorno.edi.reader;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.helpers.AttributesImpl;

public class EdiAttributes extends AttributesImpl {
    public EdiAttributes() {
    }

    public EdiAttributes(String... pairs) {
        for (int i = 0; i < pairs.length; ) {
            this.add(pairs[i++], pairs[i++]);
        }
    }

    public void add(String name, String value) {
        name = name.intern();
        if(StringUtils.isNotEmpty(value))
            this.addAttribute("", name, name, "String", value);
    }
}
