package io.quarkus.qute.deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
@Named
public class Hello {

    public String ping() {
        return "pong";
    }

}