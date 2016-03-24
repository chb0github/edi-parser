package org.bongiorno.edi.reader.tokenizer;


import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class CompositeElement extends Element {

    private List<String> parts;

    public CompositeElement(String str, Character compositeDelimiter) {
        super(str);
        this.parts = Arrays.asList(str.split(Pattern.quote(compositeDelimiter.toString()), -1));
    }

    public List<String> getParts() {
        return parts;
    }

    public String getPart(Integer position) {
        return parts.get(position);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CompositeElement that = (CompositeElement) o;

        if (parts != null ? !parts.equals(that.parts) : that.parts != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (parts != null ? parts.hashCode() : 0);
        return result;
    }
}
