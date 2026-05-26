package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;

public class AddJobResourceDecorator extends BaseAddDeploymentResourceDecorator<Job, JobBuilder, PlatformConfiguration> {

    public AddJobResourceDecorator(String name, PlatformConfiguration config, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.Job, config, toRemove);
    }

    @Override
    protected JobBuilder builderWithName(String name) {
        return new JobBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(JobBuilder builder) {
        final var spec = builder.editOrNewSpec();

        // match labels for selector
        initSelectorMatchLabels(spec.editOrNewSelector())
                .endSelector();

        // configure job pod and container from template
        configurePodSpec(spec.editOrNewTemplate().editOrNewSpec())
                .endSpec().endTemplate();

        // initialize from config
        final var config = config().job();
        spec.withSuspend(config.suspend());
        initFromConfig(spec, config);

        spec.endSpec();
    }
}
