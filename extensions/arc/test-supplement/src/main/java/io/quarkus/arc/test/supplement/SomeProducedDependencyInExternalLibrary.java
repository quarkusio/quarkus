package io.quarkus.arc.test.supplement;

public class SomeProducedDependencyInExternalLibrary {
    private final SomeDependencyInExternalLibrary dependency;

    public SomeProducedDependencyInExternalLibrary(SomeDependencyInExternalLibrary dependency) {
        this.dependency = dependency;
    }

    public String hello() {
        return "Produced: " + dependency.hello();
    }
}
