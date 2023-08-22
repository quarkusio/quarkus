package io.quarkus.test.component;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

// using normal scope so that client proxy is required, so the class must:
// - not be `final`
// - not have non-`private` `final` methods
// - not have a `private` constructor
// all these rules are deliberately broken to trigger ArC bytecode transformation
@ApplicationScoped
public final class ComponentFoo {

    @ConfigProperty(name = "bar", defaultValue = "baz")
    String bar;

    private ComponentFoo() {
    }

    final String ping() {
        return bar;
    }

}
