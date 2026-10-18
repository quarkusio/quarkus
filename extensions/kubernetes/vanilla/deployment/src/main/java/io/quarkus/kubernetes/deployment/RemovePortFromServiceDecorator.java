package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.AddServiceResourceDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecFluent;

public class RemovePortFromServiceDecorator extends NamedResourceDecorator<ServiceSpecFluent> {

    private final String portNameToRemove;

    public RemovePortFromServiceDecorator(String name, String portNameToRemove) {
        super(name);
        this.portNameToRemove = portNameToRemove;
    }

    @Override
    public void andThenVisit(ServiceSpecFluent service, ObjectMeta resourceMeta) {
        service.removeMatchingFromPorts(p -> ((ServicePortBuilder) p).getName().equals(portNameToRemove));
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddServiceResourceDecorator.class };
    }

}
