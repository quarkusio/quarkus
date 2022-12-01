package org.acme.app;

import org.acme.common.health.HealthStatus;
import org.acme.common.health.HealthStatusProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
public class Service1 {
    @Inject
    @Named(HealthStatusProducer.STATUS_1)
    HealthStatus healthStatus;

    public void updateHealthStatus(boolean healthy){
        healthStatus.setHealthy(healthy);
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }
}
