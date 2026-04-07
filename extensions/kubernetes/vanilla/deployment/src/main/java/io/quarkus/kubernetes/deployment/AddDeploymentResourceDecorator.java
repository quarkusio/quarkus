
package io.quarkus.kubernetes.deployment;

import java.util.HashMap;
import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent;

public class AddDeploymentResourceDecorator extends BaseAddDeploymentResourceDecorator<Deployment, DeploymentBuilder, Void> {
    public AddDeploymentResourceDecorator(String name, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.Deployment, null, toRemove);
    }

    @Override
    protected DeploymentBuilder builderWithName(String name) {
        return new DeploymentBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(DeploymentBuilder builder, Void config) {
        DeploymentFluent<?>.SpecNested<DeploymentBuilder> spec = builder.editOrNewSpec();

        spec.editOrNewSelector()
                .endSelector()
                .editOrNewTemplate()
                .editOrNewSpec()
                .endSpec()
                .editOrNewMetadata()
                .endMetadata()
                .endTemplate();

        // defaults for:
        // - replicas
        if (spec.getReplicas() == null) {
            spec.withReplicas(1);
        }
        // - match labels
        if (spec.buildSelector().getMatchLabels() == null) {
            spec.editSelector().withMatchLabels(new HashMap<>()).endSelector();
        }
        // - termination grace period seconds
        if (spec.buildTemplate().getSpec().getTerminationGracePeriodSeconds() == null) {
            spec.editTemplate().editSpec().withTerminationGracePeriodSeconds(10L).endSpec().endTemplate();
        }
        // - container
        if (!containsContainerWithName(spec)) {
            spec.editTemplate().editSpec().addNewContainer().withName(name()).endContainer().endSpec().endTemplate();
        }

        spec.endSpec();
    }

    private boolean containsContainerWithName(DeploymentFluent<?>.SpecNested<DeploymentBuilder> spec) {
        List<Container> containers = spec.buildTemplate().getSpec().getContainers();
        return containers != null && containers.stream().anyMatch(c -> name().equals(c.getName()));
    }
}
