
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.AddToMatchingLabelsDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecFluent;

public class AddSelectorToDeploymentDecorator extends NamedResourceDecorator<DeploymentSpecFluent<?>> {

    public AddSelectorToDeploymentDecorator(String name) {
        super(name);
    }

    @Override
    public void andThenVisit(DeploymentSpecFluent<?> spec, ObjectMeta meta) {
        if (!spec.hasSelector()) {
            spec.withNewSelector()
                    .endSelector();
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }

    @Override
    public Class<? extends Decorator>[] before() {
        return new Class[] { AddToMatchingLabelsDecorator.class };
    }
}
