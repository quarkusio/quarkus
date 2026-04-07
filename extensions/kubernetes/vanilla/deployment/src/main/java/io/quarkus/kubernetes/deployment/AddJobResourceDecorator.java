package io.quarkus.kubernetes.deployment;

import java.util.HashMap;
import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobFluent;

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
        JobFluent<?>.SpecNested<JobBuilder> spec = builder.editOrNewSpec();

        spec.editOrNewSelector()
                .endSelector()
                .editOrNewTemplate()
                .editOrNewSpec()
                .endSpec()
                .endTemplate();

        // defaults for:
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

        spec.withSuspend(config.suspend());
        spec.withCompletionMode(config.completionMode().name());
        spec.editTemplate().editSpec().withRestartPolicy(config.restartPolicy().name()).endSpec().endTemplate();
        config.parallelism().ifPresent(spec::withParallelism);
        config.completions().ifPresent(spec::withCompletions);
        config.backoffLimit().ifPresent(spec::withBackoffLimit);
        config.activeDeadlineSeconds().ifPresent(spec::withActiveDeadlineSeconds);
        config.ttlSecondsAfterFinished().ifPresent(spec::withTtlSecondsAfterFinished);

        spec.endSpec();
    }

    private boolean containsContainerWithName(JobFluent<?>.SpecNested<JobBuilder> spec) {
        List<Container> containers = spec.buildTemplate().getSpec().getContainers();
        return containers == null || containers.stream().anyMatch(c -> name().equals(c.getName()));
    }
}
