package io.quarkus.arquillian.test;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;

@Dependent
public class SimpleClass {

    @Inject
    Config config;

    @Inject
    Foo foo;

}
