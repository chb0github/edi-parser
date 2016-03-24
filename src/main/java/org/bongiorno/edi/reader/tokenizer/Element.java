package org.bongiorno.edi.reader.tokenizer;

public class Element {
    private String str;

    public Element(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }

    public static Element create(String elementString, Character compositeDelimiter, Character repeatDelimiter) {
        if (elementString.indexOf(repeatDelimiter) == -1) {
            return create(elementString, compositeDelimiter);
        } else {
            return new RepeatingElement(elementString, compositeDelimiter, repeatDelimiter);
        }
    }

    public static Element create(String elementString, Character compositeDelimiter) {
        if (elementString.indexOf(compositeDelimiter) == -1) {
            return new Element(elementString);
        } else {
            return new CompositeElement(elementString, compositeDelimiter);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Element element = (Element) o;

        if (str != null ? !str.equals(element.str) : element.str != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return str != null ? str.hashCode() : 0;
    }
}
