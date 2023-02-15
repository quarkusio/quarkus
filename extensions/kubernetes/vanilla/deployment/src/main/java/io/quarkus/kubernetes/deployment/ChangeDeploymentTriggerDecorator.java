package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.openshift.decorator.ApplyDeploymentTriggerDecorator;

public class ChangeDeploymentTriggerDecorator extends ApplyDeploymentTriggerDecorator {

    public ChangeDeploymentTriggerDecorator(String containerName, String imageStreamTag) {
        super(containerName, imageStreamTag);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyDeploymentTriggerDecorator.class, RemoveDeploymentTriggerDecorator.class };
    }

}
