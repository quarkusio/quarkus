package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;

public class AddJobResourceDecorator extends BaseAddDeploymentResourceDecorator<Job, JobBuilder, JobConfig> {

    public AddJobResourceDecorator(String name, JobConfig config, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.Job, config, toRemove);
    }

    @Override
    protected JobBuilder builderWithName(String name) {
        return new JobBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(JobBuilder builder, JobConfig config) {
        final var spec = builder.editOrNewSpec();

        // match labels for selector
        initSelectorMatchLabels(spec.editOrNewSelector())
                .endSelector();

        // ensure defaults on template spec
        podSpecDefaults(spec.editOrNewTemplate().editOrNewSpec())
                .endSpec().endTemplate();

        // initialize from config
        spec.withSuspend(config.suspend());
        initFromConfig(spec, config);

        spec.endSpec();
    }
}
