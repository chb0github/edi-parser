package org.bongiorno.edi.reader.tokenizer;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class RepeatingElement extends Element {

    private List<Element> elements;

    public RepeatingElement(String str, Character compositeDelimiter, Character repetitionDelimiter) {
        super(str);
        String[] subElements = str.split(Pattern.quote(repetitionDelimiter.toString()), -1);
        elements = stream(subElements)
                .map(se -> Element.create(se, compositeDelimiter))
                .collect(toList());
    }

    public List<Element> getElements() {
        return elements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RepeatingElement that = (RepeatingElement) o;

        if (elements != null ? !elements.equals(that.elements) : that.elements != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (elements != null ? elements.hashCode() : 0);
        return result;
    }
}
