package io.quarkus.arc.test.unused;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Charlie {

    public String ping() {
        return "ok";
    }
}
