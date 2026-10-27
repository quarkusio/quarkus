package io.quarkus.arquillian.test;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.smallrye.config.Config;

@Dependent
public class SimpleClass {

    @Inject
    Config config;

    @Inject
    Foo foo;

}
