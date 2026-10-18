package io.quarkus.annotation.processor.documentation.config.discovery;

import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;

public record ParsedJavadocSection(String title, String details, JavadocFormat format, String deprecated) {

    public static ParsedJavadocSection empty() {
        return new ParsedJavadocSection(null, null, null, null);
    }
}