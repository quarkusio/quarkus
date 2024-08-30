package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Map;

public record JavadocElements(Extension extension, Map<String, JavadocElement> elements) {

    public record JavadocElement(String description, String since, String deprecated, String rawJavadoc) {
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
