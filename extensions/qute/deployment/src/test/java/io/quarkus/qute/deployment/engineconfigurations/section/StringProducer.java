package io.quarkus.qute.deployment.engineconfigurations.section;

import javax.enterprise.inject.Produces;

public class StringProducer {

    @Produces
    public String bar() {
        return "BAR!";
    }

}
