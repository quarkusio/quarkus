package io.quarkus.kubernetes.deployment;

import java.util.Collections;

import io.dekorate.deps.kubernetes.api.model.ObjectMeta;
import io.dekorate.deps.openshift.api.model.DeploymentConfigSpecFluent;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.openshift.decorator.ApplyDeploymentTriggerDecorator;

public class RemoveDeploymentTriggerDecorator extends NamedResourceDecorator<DeploymentConfigSpecFluent<?>> {

    @Override
    public void andThenVisit(DeploymentConfigSpecFluent<?> deploymentConfigSpec, ObjectMeta objectMeta) {
        deploymentConfigSpec.withTriggers(Collections.emptyList());
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyDeploymentTriggerDecorator.class };
    }
}
