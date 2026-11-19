package io.quarkus.kubernetes.deployment;

import java.util.Comparator;

import io.dekorate.kubernetes.decorator.AddServiceResourceDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpecFluent;

/**
 * Sorts service ports by name (nulls last), then port number, then protocol.
 */
public class SortServicePortsDecorator extends NamedResourceDecorator<ServiceSpecFluent<?>> {

    public SortServicePortsDecorator(String name) {
        super(name);
    }

    @Override
    public void andThenVisit(ServiceSpecFluent<?> service, ObjectMeta resourceMeta) {
        if (service.hasPorts()) {
            var sortedPorts = service.buildPorts().stream()
                    .sorted(Comparator
                            .comparing(ServicePort::getName, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(ServicePort::getPort, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(p -> p.getProtocol() != null ? p.getProtocol() : ""))
                    .toList();

            service.withPorts(sortedPorts);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddServiceResourceDecorator.class };
    }
}
