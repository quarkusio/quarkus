package io.quarkus.test.component.declarative;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SomeBean implements SomeInterface {

    private boolean val;

    public boolean ping() {
        return val;
    }

    @PostConstruct
    void init() {
        val = true;
    }

}
