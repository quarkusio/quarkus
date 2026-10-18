package io.quarkus.gradle.dsl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.internal.DefaultAttributes;

public class Manifest {

    private Attributes attributes = new DefaultAttributes();
    private Map<String, Attributes> sections = new LinkedHashMap<>();

    public Attributes getAttributes() {
        return attributes;
    }

    public Map<String, Attributes> getSections() {
        return sections;
    }

    public Manifest attributes(Map<String, String> attributes) {
        this.attributes.putAll(attributes);
        return this;
    }

    public Manifest attributes(Map<String, String> attributes, String section) {
        if (!this.sections.containsKey(section)) {
            this.sections.put(section, new DefaultAttributes());
        }
        this.sections.get(section).putAll(attributes);
        return this;
    }
}
