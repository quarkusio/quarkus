package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ObjectMeta;
import io.dekorate.deps.kubernetes.api.model.ServiceSpecFluent;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;

/**
 * A decorator for applying a serviceType to the container
 */
public class ApplyServiceTypeDecorator extends NamedResourceDecorator<ServiceSpecFluent> {

    private final String type;

    public ApplyServiceTypeDecorator(String name, String type) {
        super(name);
        this.type = type;
    }

    @Override
    public void andThenVisit(ServiceSpecFluent service, ObjectMeta resourceMeta) {
        service.withType(type);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }

}
