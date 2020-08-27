package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ObjectMeta;
import io.dekorate.deps.kubernetes.api.model.apps.DeploymentSpecFluent;
import io.dekorate.kubernetes.decorator.ApplyLabelSelectorDecorator;
import io.dekorate.kubernetes.decorator.Decorator;

public class DeploymentRemoveLabelSelectorDecorator extends RemoveLabelSelectorDecorator<DeploymentSpecFluent> {

    public DeploymentRemoveLabelSelectorDecorator(String label) {
        super(label);
    }

    @Override
    public void andThenVisit(DeploymentSpecFluent spec, ObjectMeta objectMeta) {
        DeploymentSpecFluent.SelectorNested selector = spec.editSelector();
        selector.removeFromMatchLabels(label);
        selector.endSelector();
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ApplyLabelSelectorDecorator.class };
    }
}
