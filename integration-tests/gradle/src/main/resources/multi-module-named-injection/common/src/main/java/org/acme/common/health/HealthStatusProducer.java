package org.acme.common.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

@ApplicationScoped
public class HealthStatusProducer {

    public static final String STATUS_1 = "Status1";
    public static final String STATUS_2 = "Status2";

    @Produces
    @ApplicationScoped
    @Named(STATUS_1)
    public HealthStatus produceStatus1() {
        return new HealthStatus();
    }

    @Produces
    @ApplicationScoped
    @Named(STATUS_2)
    public HealthStatus produceStatus2() {
        return new HealthStatus();
    }
}
