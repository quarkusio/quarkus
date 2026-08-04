package io.quarkus.extest.runtime;

public class PublicMethodsReflectionObject {

    public String publicMethod() {
        return "public";
    }

    @SuppressWarnings("unused")
    private String privateMethod() {
        return "private";
    }
}
