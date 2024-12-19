package io.quarkus.micrometer.opentelemetry.services;

import jakarta.inject.Singleton;

import io.micrometer.common.annotation.ValueResolver;

@Singleton
public class TestValueResolver implements ValueResolver {
    @Override
    public String resolve(Object parameter) {
        return "prefix_" + parameter;
    }
}
