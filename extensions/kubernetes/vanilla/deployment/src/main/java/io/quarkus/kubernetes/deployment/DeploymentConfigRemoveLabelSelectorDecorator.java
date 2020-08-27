package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ObjectMeta;
import io.dekorate.deps.openshift.api.model.DeploymentConfigSpecFluent;

public class DeploymentConfigRemoveLabelSelectorDecorator extends RemoveLabelSelectorDecorator<DeploymentConfigSpecFluent> {

    public DeploymentConfigRemoveLabelSelectorDecorator(String label) {
        super(label);
    }

    @Override
    public void andThenVisit(DeploymentConfigSpecFluent spec, ObjectMeta objectMeta) {
        spec.removeFromSelector(label);
    }

}
