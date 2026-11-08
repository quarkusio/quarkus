package io.quarkus.kubernetes.deployment;

import java.util.Comparator;

import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddPortDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.fabric8.kubernetes.api.model.ContainerFluent;

/**
 * Decorator that sorts container ports alphabetically by name.
 * This ensures deterministic manifest generation.
 * Runs after all ports have been added.
 */
public class SortContainerPortsDecorator extends ApplicationContainerDecorator<ContainerFluent<?>> {

    @Override
    public void andThenVisit(ContainerFluent<?> container) {
        if (container.hasPorts()) {
            // Get current ports, sort them by name, and rebuild the list
            var sortedPorts = container.buildPorts().stream()
                    .sorted(Comparator.comparing(p -> p.getName()))
                    .toList();

            // Clear and re-add in sorted order
            container.withPorts(sortedPorts);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        // Run after all port decorators have added their ports
        return new Class[] { AddPortDecorator.class, AddEnvVarDecorator.class };
    }
}
