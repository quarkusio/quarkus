package io.quarkus.it.opentelemetry;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Book(String author, String title) {

}
