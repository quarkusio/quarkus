package io.quarkus.spring.data.runtime;

public class FunctionalityNotImplemented extends RuntimeException {

    public FunctionalityNotImplemented(String className, String methodName) {
        super("Method " + methodName + " of class " + className + " from Spring Data has not yet been implemented");
    }
}
