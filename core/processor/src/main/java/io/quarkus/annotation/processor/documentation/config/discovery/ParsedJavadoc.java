package io.quarkus.annotation.processor.documentation.config.discovery;

import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;

public record ParsedJavadoc(String description, JavadocFormat format, String since, String deprecated) {

    public static ParsedJavadoc empty() {
        return new ParsedJavadoc(null, null, null, null);
    }

    public boolean isEmpty() {
        return description == null || description.isBlank();
    }
}