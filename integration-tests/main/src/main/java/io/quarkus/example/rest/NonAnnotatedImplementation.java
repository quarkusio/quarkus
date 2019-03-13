package io.quarkus.example.rest;

public class NonAnnotatedImplementation implements AnnotatedInterface {
    @Override
    public String get() {
        return "interface endpoint";
    }
}
