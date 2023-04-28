package io.quarkus.flyway.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import io.quarkus.runtime.annotations.PreStart;

@ApplicationScoped
@PreStart("flyway-init-task")
public class FlywayInitTask implements Runnable {

    @Inject
    Instance<FlywayContainer> flywayContainers;

    @Inject
    @ConfigProperty(name = "quarkus.flyway.enabled")
    boolean enabled;

    @Override
    public void run() {
        if (!enabled) {
            return;
        }

        for (FlywayContainer flywayContainer : flywayContainers) {
            Flyway flyway = flywayContainer.getFlyway();
            if (flywayContainer.isCleanAtStart()) {
                flyway.clean();
            }
            if (flywayContainer.isValidateAtStart()) {
                flyway.validate();
            }
            if (flywayContainer.isRepairAtStart()) {
                flyway.repair();
            }
            if (flywayContainer.isMigrateAtStart()) {
                flyway.migrate();
            }
        }
    }
}
