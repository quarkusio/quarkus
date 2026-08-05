package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ContainerFluent;
import io.fabric8.kubernetes.api.model.Quantity;

public interface ResourcesConfig {
    /**
     * Limits Requirements
     */
    ResourcesRequirementsConfig limits();

    /**
     * Requests Requirements
     */
    ResourcesRequirementsConfig requests();

    interface ResourcesRequirementsConfig {
        /**
         * CPU Requirements
         */
        Optional<String> cpu();

        /**
         * Memory Requirements
         */
        Optional<String> memory();
    }

    String CPU = "cpu";
    String MEMORY = "memory";

    default void applyToContainer(ContainerFluent<?> container) {
        final var requests = requests();
        var mem = requests.memory().orElse(null);
        var cpu = requests.cpu().orElse(null);
        if (mem != null || cpu != null) {
            final var resources = container.withNewResources();
            if (mem != null) {
                resources.addToRequests(MEMORY, new Quantity(mem));
            }
            if (cpu != null) {
                resources.addToRequests(CPU, new Quantity(cpu));
            }
            resources.endResources();
        }

        final var limits = limits();
        mem = limits.memory().orElse(null);
        cpu = limits.cpu().orElse(null);
        if (mem != null || cpu != null) {
            final var resources = container.editOrNewResources();
            if (mem != null) {
                resources.addToLimits(MEMORY, new Quantity(mem));
            }
            if (cpu != null) {
                resources.addToLimits(CPU, new Quantity(cpu));
            }
            resources.endResources();
        }
    }
}
