package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.ApplyReplicasDecorator;
import io.dekorate.kubernetes.decorator.Decorator;

public class KubernetesApplyReplicasDecorator extends ApplyReplicasDecorator {

    public KubernetesApplyReplicasDecorator(int replicas) {
        super(replicas);
    }

    public KubernetesApplyReplicasDecorator(String deploymentName, int replicas) {
        super(deploymentName, replicas);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyReplicasDecorator.class };
    }

}
