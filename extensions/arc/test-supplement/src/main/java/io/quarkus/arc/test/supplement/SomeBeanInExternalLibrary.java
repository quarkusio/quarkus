package io.quarkus.arc.test.supplement;

import jakarta.enterprise.context.Dependent;

@Dependent
public class SomeBeanInExternalLibrary implements SomeInterfaceInExternalLibrary {
    @Override
    public String hello() {
        return "Hello";
    }
}
