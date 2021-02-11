package io.quarkus.arc.test.config;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SomeBeanUsingConfig {

    // deliberately missing @Inject
    @ConfigProperty(name = "something")
    String foo;

    public String getFoo() {
        return foo;
    }
}
