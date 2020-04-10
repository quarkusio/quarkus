package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.openshift.decorator.ApplyReplicasDecorator;

public class OpenshiftApplyReplicasDecorator extends ApplyReplicasDecorator {

    public OpenshiftApplyReplicasDecorator(int replicas) {
        super(replicas);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyReplicasDecorator.class };
    }

}
