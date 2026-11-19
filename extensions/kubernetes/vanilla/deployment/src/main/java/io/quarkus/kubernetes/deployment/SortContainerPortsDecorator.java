package io.quarkus.kubernetes.deployment;

import java.util.Comparator;

import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddPortDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.fabric8.kubernetes.api.model.ContainerFluent;
import io.fabric8.kubernetes.api.model.ContainerPort;

/**
 * Sorts container ports by name (nulls last), then port number, then protocol.
 */
public class SortContainerPortsDecorator extends ApplicationContainerDecorator<ContainerFluent<?>> {

    @Override
    public void andThenVisit(ContainerFluent<?> container) {
        if (container.hasPorts()) {
            var sortedPorts = container.buildPorts().stream()
                    .sorted(Comparator
                            .comparing(ContainerPort::getName, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(ContainerPort::getContainerPort, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(p -> p.getProtocol() != null ? p.getProtocol() : ""))
                    .toList();

            container.withPorts(sortedPorts);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddPortDecorator.class, AddEnvVarDecorator.class };
    }
}
