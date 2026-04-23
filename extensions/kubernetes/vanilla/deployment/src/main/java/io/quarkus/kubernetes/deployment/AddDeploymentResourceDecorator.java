
package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

public class AddDeploymentResourceDecorator
        extends BaseAddDeploymentResourceDecorator<Deployment, DeploymentBuilder, ReplicasAware> {
    public AddDeploymentResourceDecorator(String name, ReplicasAware config, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.Deployment, config, toRemove);
    }

    @Override
    protected DeploymentBuilder builderWithName(String name) {
        return new DeploymentBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(DeploymentBuilder builder, ReplicasAware config) {
        final var spec = builder.editOrNewSpec();

        // match labels for selector
        initMatchLabels(spec.editOrNewSelector())
                .endSelector();

        // replicas
        spec.withReplicas(replicas(spec.getReplicas(), config));

        // ensure defaults on template spec
        podSpecDefaults(spec.editOrNewTemplate().editOrNewSpec())
                .endSpec().endTemplate();

        spec.endSpec();
    }
}
