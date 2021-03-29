package io.quarkus.arc.test.interceptors.defaultmethod;

public interface DefaultMethodInterface {

    @NextBinding // This annotation should be ignored
    default String defaultMethod() {
        return "default method";
    }

    @NextBinding // This annotation should be ignored
    String ping();
}
