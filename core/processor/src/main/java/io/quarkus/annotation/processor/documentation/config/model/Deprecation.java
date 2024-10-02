package io.quarkus.annotation.processor.documentation.config.model;

public record Deprecation(String since, String replacement, String reason) {

}
