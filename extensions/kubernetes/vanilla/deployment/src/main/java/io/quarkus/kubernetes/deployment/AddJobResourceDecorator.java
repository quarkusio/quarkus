package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.JOB;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListFluent;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobFluent;

public class AddJobResourceDecorator extends ResourceProvidingDecorator<KubernetesListFluent<?>> {

    private final String name;
    private final JobConfig config;

    public AddJobResourceDecorator(String name, JobConfig config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public void visit(KubernetesListFluent<?> list) {
        JobBuilder builder = list.buildItems().stream()
                .filter(this::containsJobResource)
                .map(replaceExistingJobResource(list))
                .findAny()
                .orElseGet(this::createJobResource)
                .accept(JobBuilder.class, this::initJobResourceWithDefaults);

        list.addToItems(builder.build());
    }

    private boolean containsJobResource(HasMetadata metadata) {
        return JOB.equalsIgnoreCase(metadata.getKind()) && name.equals(metadata.getMetadata().getName());
    }

    private void initJobResourceWithDefaults(JobBuilder builder) {
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
            spec.editTemplate().editSpec().addNewContainer().withName(name).endContainer().endSpec().endTemplate();
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

    private JobBuilder createJobResource() {
        return new JobBuilder().withNewMetadata().withName(name).endMetadata();
    }

    private Function<HasMetadata, JobBuilder> replaceExistingJobResource(KubernetesListFluent<?> list) {
        return metadata -> {
            list.removeFromItems(metadata);
            return new JobBuilder((Job) metadata);
        };
    }

    private boolean containsContainerWithName(JobFluent<?>.SpecNested<JobBuilder> spec) {
        List<Container> containers = spec.buildTemplate().getSpec().getContainers();
        return containers == null || containers.stream().anyMatch(c -> name.equals(c.getName()));
    }
}
