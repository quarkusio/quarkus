package io.quarkus.arc.test.interceptors.defaultmethod;

public interface DefaultMethodInterface {

    default String defaultMethod() {
        return "default method";
    }
}
