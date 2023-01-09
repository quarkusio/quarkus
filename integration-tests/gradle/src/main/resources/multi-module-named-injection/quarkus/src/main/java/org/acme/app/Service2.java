package org.acme.app;

import org.acme.common.health.HealthStatus;
import org.acme.common.health.HealthStatusProducer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class Service2 {
    @Inject
    @Named(HealthStatusProducer.STATUS_2)
    HealthStatus healthStatus;

    public void updateHealthStatus(boolean healthy) {
        healthStatus.setHealthy(healthy);
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }
}
