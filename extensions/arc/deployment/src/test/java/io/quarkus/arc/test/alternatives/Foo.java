package io.quarkus.arc.test.alternatives;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Foo {

    public String ping() {
        return getClass().getName();
    }

}