package io.quarkus.flyway.runtime;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import io.quarkus.runtime.annotations.PreStart;

@ApplicationScoped
@PreStart("flyway-init-task")
public class FlywayInitTask implements Runnable {

    /**
     * While there is no clear way of defining the order in which containers need to
     * processed, there are integration tests that do require the `Default` datasource
     * to be processed first.
     **/
    @Inject
    @Default
    Instance<FlywayContainer> defaultFlywayContainer;

    @Inject
    @Any
    Instance<FlywayContainer> flywayContainers;

    @Inject
    @ConfigProperty(name = "quarkus.flyway.enabled")
    boolean enabled;

    @Override
    public void run() {
        if (!enabled) {
            return;
        }

        Optional<String> defaultFlywayContainerId = Optional.empty();
        if (defaultFlywayContainer.isResolvable()) {
            FlywayContainer container = defaultFlywayContainer.get();
            defaultFlywayContainerId = Optional.of(container.getId());
            initialize(container);
        }

        for (FlywayContainer flywayContainer : flywayContainers) {
            if (defaultFlywayContainerId.filter(id -> id.equals(flywayContainer.getId())).isPresent()) {
                continue;
            }
            initialize(flywayContainer);
        }
    }

    private void initialize(FlywayContainer flywayContainer) {
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
