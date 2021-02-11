package io.quarkus.arc.test.unused;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Charlie {

    public String ping() {
        return "ok";
    }
}
