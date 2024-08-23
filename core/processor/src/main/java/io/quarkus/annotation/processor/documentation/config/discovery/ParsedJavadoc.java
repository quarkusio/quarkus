package io.quarkus.annotation.processor.documentation.config.discovery;

public record ParsedJavadoc(String description, String since, JavadocFormat originalFormat) {

    public static ParsedJavadoc empty() {
        return new ParsedJavadoc(null, null, null);
    }

    public boolean isEmpty() {
        return description == null || description.isBlank();
    }
}