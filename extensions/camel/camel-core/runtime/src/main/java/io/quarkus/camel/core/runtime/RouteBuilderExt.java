package io.quarkus.camel.core.runtime;

import org.apache.camel.builder.RouteBuilder;

public abstract class RouteBuilderExt extends RouteBuilder {

    private RuntimeRegistry registry;

    public void setRegistry(RuntimeRegistry registry) {
        this.registry = registry;
    }

    public void bind(String name, Object object) {
        registry.bind(name, object);
    }

    public void bind(String name, Class<?> clazz, Object object) {
        registry.bind(name, clazz, object);
    }
}
