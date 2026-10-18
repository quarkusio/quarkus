package io.quarkus.arc.test.alternatives;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Foo {

    public String ping() {
        return getClass().getName();
    }

}
