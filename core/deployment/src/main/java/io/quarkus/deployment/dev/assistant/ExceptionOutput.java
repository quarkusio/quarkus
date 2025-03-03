package io.quarkus.deployment.dev.assistant;

public record ExceptionOutput(String response, String explanation, String diff, String manipulatedContent) {
}
