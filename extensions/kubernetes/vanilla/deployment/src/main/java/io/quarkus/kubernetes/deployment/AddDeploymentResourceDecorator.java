
package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

public class AddDeploymentResourceDecorator
        extends BaseAddDeploymentResourceDecorator<Deployment, DeploymentBuilder, PlatformConfiguration> {
    public AddDeploymentResourceDecorator(String name, PlatformConfiguration config, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.Deployment, config, toRemove);
    }

    @Override
    protected DeploymentBuilder builderWithName(String name) {
        return new DeploymentBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(DeploymentBuilder builder) {
        final var spec = builder.editOrNewSpec();

        // match labels for selector
        initSelectorMatchLabels(spec.editOrNewSelector())
                .endSelector();

        // replicas
        spec.withReplicas(replicas(spec.getReplicas(), replicasAwareOrNull()));

        // configure main application pod and container
        configurePodSpec(spec.editOrNewTemplate().editOrNewSpec())
                .endSpec().endTemplate();

        spec.endSpec();
    }
}
