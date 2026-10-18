package io.quarkus.kubernetes.deployment;

import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;

public class AddDeploymentConfigResourceDecorator
        extends BaseAddDeploymentResourceDecorator<DeploymentConfig, DeploymentConfigBuilder, OpenShiftConfig> {

    public AddDeploymentConfigResourceDecorator(String name, OpenShiftConfig config, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.DeploymentConfig, config, toRemove);
    }

    @Override
    protected DeploymentConfigBuilder builderWithName(String name) {
        return new DeploymentConfigBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(DeploymentConfigBuilder builder) {
        final var spec = builder.editOrNewSpec();

        // replicas
        spec.withReplicas(replicas(spec.getReplicas(), replicasAwareOrNull()));

        // configure main application pod and container
        configurePodSpec(spec.editOrNewTemplate().editOrNewSpec())
                .endSpec().endTemplate();

        spec.endSpec();
    }
}
