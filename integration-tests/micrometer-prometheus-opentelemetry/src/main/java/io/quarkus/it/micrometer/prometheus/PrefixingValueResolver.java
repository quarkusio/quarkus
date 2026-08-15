package io.quarkus.it.micrometer.prometheus;

import jakarta.inject.Singleton;

import io.micrometer.common.annotation.ValueResolver;

@Singleton
public class PrefixingValueResolver implements ValueResolver {
    @Override
    public String resolve(Object parameter) {
        return "prefix " + parameter;
    }
}
