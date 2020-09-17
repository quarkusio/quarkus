
package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.HTTPGetActionFluent;
import io.dekorate.kubernetes.decorator.AddLivenessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddReadinessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;

public class ApplyHttpGetActionPortDecorator extends ApplicationContainerDecorator<HTTPGetActionFluent<?>> {

    private final Integer port;

    public ApplyHttpGetActionPortDecorator(Integer port) {
        super(ANY, ANY); //We need to apply this to all deployments and all containers.
        this.port = port;
    }

    @Override
    public void andThenVisit(HTTPGetActionFluent<?> action) {
        if (port == null) {
            // workaround to make sure we don't get a NPE
            action.withNewPort((String) null);
        } else {
            action.withNewPort(port);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class,
                AddLivenessProbeDecorator.class, AddReadinessProbeDecorator.class };
    }
}
