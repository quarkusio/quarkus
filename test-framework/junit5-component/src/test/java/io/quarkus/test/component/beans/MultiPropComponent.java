package io.quarkus.test.component.beans;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MultiPropComponent {

    @ConfigProperty(name = "foo")
    String foo;

    @ConfigProperty(name = "bar")
    String bar;

    public String getFooBar() {
        return foo + bar;
    }

}
