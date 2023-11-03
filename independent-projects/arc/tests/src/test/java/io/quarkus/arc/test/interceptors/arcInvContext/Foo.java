package io.quarkus.arc.test.interceptors.arcInvContext;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@SomeBinding
public class Foo {

    public String ping() {
        return Foo.class.getSimpleName();
    }
}
