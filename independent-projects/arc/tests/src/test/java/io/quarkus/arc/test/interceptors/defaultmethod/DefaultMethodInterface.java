package io.quarkus.arc.test.interceptors.defaultmethod;

public interface DefaultMethodInterface {

    @NextBinding
    default String defaultMethod() {
        return "default method";
    }

    @NextBinding // ignored on abstract interface methods
    String ping();
}
