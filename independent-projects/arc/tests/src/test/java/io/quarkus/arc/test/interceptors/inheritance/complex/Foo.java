package io.quarkus.arc.test.interceptors.inheritance.complex;

import javax.enterprise.context.ApplicationScoped;

@Binding
@ApplicationScoped
public class Foo {
    public String ping() {
        return "pong";
    }
}
