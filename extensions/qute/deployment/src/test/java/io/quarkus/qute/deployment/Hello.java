package io.quarkus.qute.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named
public class Hello {

    public String ping() {
        return "pong";
    }

}
