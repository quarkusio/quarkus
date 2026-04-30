package io.quarkus.kubernetes.deployment;

import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;

public class AddDeploymentConfigResourceDecorator
        extends BaseAddDeploymentResourceDecorator<DeploymentConfig, DeploymentConfigBuilder, ReplicasAware> {

    public AddDeploymentConfigResourceDecorator(String name, ReplicasAware config, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.DeploymentConfig, config, toRemove);
    }

    @Override
    protected DeploymentConfigBuilder builderWithName(String name) {
        return new DeploymentConfigBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(DeploymentConfigBuilder builder, ReplicasAware config) {
        final var spec = builder.editOrNewSpec();

        // replicas
        spec.withReplicas(replicas(spec.getReplicas(), config));

        // ensure defaults on template spec
        podSpecDefaults(spec.editOrNewTemplate().editOrNewSpec())
                .endSpec().endTemplate();

        spec.endSpec();
    }
}
