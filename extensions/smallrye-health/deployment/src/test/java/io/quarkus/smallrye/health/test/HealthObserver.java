package io.quarkus.smallrye.health.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;

import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import io.smallrye.health.api.event.HealthStatusChangeEvent;

@ApplicationScoped
public class HealthObserver {

    public static int healthChangeCounter = 0;
    public static int readinessChangeCounter = 0;
    public static int livenessChangeCounter = 0;

    public void observeHealthChange(@Observes @Default HealthStatusChangeEvent event) {
        healthChangeCounter++;
    }

    public void observeReadinessChange(@Observes @Readiness HealthStatusChangeEvent event) {
        readinessChangeCounter++;
    }

    public void observeLivenessChange(@Observes @Liveness HealthStatusChangeEvent event) {
        livenessChangeCounter++;
    }
}
