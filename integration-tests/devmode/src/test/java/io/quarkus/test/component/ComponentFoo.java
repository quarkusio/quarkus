package io.quarkus.test.component;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class ComponentFoo {

    @ConfigProperty(name = "bar", defaultValue = "baz")
    String bar;

    String ping() {
        return bar;
    }

}
