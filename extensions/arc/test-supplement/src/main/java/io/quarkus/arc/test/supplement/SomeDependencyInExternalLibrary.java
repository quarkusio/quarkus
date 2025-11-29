package io.quarkus.arc.test.supplement;

import jakarta.enterprise.context.Dependent;

@Dependent
public class SomeDependencyInExternalLibrary {
    public String hello() {
        return "Hello";
    }
}
