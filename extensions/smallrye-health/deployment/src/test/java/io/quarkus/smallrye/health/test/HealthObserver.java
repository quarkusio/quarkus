package io.quarkus.smallrye.health.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;

import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import io.smallrye.health.event.SmallRyeHealthStatusChangeEvent;

@ApplicationScoped
public class HealthObserver {

    public static int healthChangeCounter = 0;
    public static int readinessChangeCounter = 0;
    public static int livenessChangeCounter = 0;

    public void observeHealthChange(@Observes @Default SmallRyeHealthStatusChangeEvent event) {
        healthChangeCounter++;
    }

    public void observeReadinessChange(@Observes @Readiness SmallRyeHealthStatusChangeEvent event) {
        readinessChangeCounter++;
    }

    public void observeLivenessChange(@Observes @Liveness SmallRyeHealthStatusChangeEvent event) {
        livenessChangeCounter++;
    }
}
