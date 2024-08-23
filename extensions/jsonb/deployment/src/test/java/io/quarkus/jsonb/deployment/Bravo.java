package io.quarkus.jsonb.deployment;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Bravo {

    String getVal(Alpha alpha) {
        return getVal(alpha.getName());
    }

    String getVal(String name) {
        return name + "_bravo";
    }

}
