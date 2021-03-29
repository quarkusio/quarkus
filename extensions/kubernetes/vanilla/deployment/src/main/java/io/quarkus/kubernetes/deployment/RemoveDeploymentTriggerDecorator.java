package io.quarkus.kubernetes.deployment;

import java.util.Collections;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.openshift.decorator.ApplyDeploymentTriggerDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.DeploymentConfigSpecFluent;

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
