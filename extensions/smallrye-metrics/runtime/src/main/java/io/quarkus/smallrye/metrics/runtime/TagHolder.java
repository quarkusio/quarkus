package io.quarkus.smallrye.metrics.runtime;

import org.eclipse.microprofile.metrics.Tag;

/**
 * Tag class from MP Metrics API does not have a public default constructor so we use this custom wrapper
 * for passing tag definitions from processor to recorder and reconstructing the original Tag instances in runtime code.
 */
public class TagHolder {

    private String name;

    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static TagHolder from(Tag tag) {
        TagHolder result = new TagHolder();
        result.setName(tag.getTagName());
        result.setValue(tag.getTagValue());
        return result;
    }

    public Tag toTag() {
        return new Tag(name, value);
    }
}
