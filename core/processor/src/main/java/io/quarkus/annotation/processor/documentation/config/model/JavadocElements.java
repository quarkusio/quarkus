package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record JavadocElements(Extension extension, Map<String, JavadocElement> elements) {

    public record JavadocElement(String description, String since, @JsonIgnore String rawJavadoc) {
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
