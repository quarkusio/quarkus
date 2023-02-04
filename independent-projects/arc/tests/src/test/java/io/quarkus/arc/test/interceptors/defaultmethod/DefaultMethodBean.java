package io.quarkus.arc.test.interceptors.defaultmethod;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ABinding
public class DefaultMethodBean implements DefaultMethodInterface {

    @NextBinding
    public String hello() {
        return "hello";
    }

    @Override
    public String ping() {
        return "pong";
    }
}
