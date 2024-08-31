package io.quarkus.annotation.processor.documentation.config.discovery;

public record ParsedJavadocSection(String title, String details, String deprecated) {

    public static ParsedJavadocSection empty() {
        return new ParsedJavadocSection(null, null, null);
    }
}