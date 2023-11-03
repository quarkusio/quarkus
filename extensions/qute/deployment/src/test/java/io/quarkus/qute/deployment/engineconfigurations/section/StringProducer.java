package io.quarkus.qute.deployment.engineconfigurations.section;

import jakarta.enterprise.inject.Produces;

public class StringProducer {

    @Produces
    public String bar() {
        return "BAR!";
    }

}
