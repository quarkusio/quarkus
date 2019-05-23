package io.quarkus.arquillian.test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;

@Dependent
public class SimpleClass {

    @Inject
    Config config;

    @Inject
    Foo foo;

}