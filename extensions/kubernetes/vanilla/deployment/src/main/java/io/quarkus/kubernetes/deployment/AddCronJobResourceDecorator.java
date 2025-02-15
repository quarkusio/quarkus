
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.CRONJOB;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListFluent;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobFluent;

public class AddCronJobResourceDecorator extends ResourceProvidingDecorator<KubernetesListFluent<?>> {

    private final String name;
    private final CronJobConfig config;

    public AddCronJobResourceDecorator(String name, CronJobConfig config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public void visit(KubernetesListFluent<?> list) {
        CronJobBuilder builder = list.buildItems().stream()
                .filter(this::containsCronJobResource)
                .map(replaceExistingCronJobResource(list))
                .findAny()
                .orElseGet(this::createCronJobResource)
                .accept(CronJobBuilder.class, this::initCronJobResourceWithDefaults);

        if (Strings.isNullOrEmpty(builder.buildSpec().getSchedule())) {
            throw new IllegalArgumentException(
                    "When generating a CronJob resource, you need to specify a schedule CRON expression.");
        }

        list.addToItems(builder.build());
    }

    private boolean containsCronJobResource(HasMetadata metadata) {
        return CRONJOB.equalsIgnoreCase(metadata.getKind()) && name.equals(metadata.getMetadata().getName());
    }

    private void initCronJobResourceWithDefaults(CronJobBuilder builder) {
        CronJobFluent<?>.SpecNested<CronJobBuilder> spec = builder.editOrNewSpec();

        var jobTemplateSpec = spec
                .editOrNewJobTemplate()
                .editOrNewSpec();

        jobTemplateSpec.editOrNewSelector()
                .endSelector()
                .editOrNewTemplate()
                .editOrNewSpec()
                .endSpec()
                .endTemplate();

        // defaults for:
        // - match labels
        if (jobTemplateSpec.buildSelector().getMatchLabels() == null) {
            jobTemplateSpec.editSelector().withMatchLabels(new HashMap<>()).endSelector();
        } else {
            jobTemplateSpec.withSelector(null);
        }
        // - termination grace period seconds
        if (jobTemplateSpec.buildTemplate().getSpec().getTerminationGracePeriodSeconds() == null) {
            jobTemplateSpec.editTemplate().editSpec().withTerminationGracePeriodSeconds(10L).endSpec().endTemplate();
        }
        // - container
        if (!containsContainerWithName(spec)) {
            jobTemplateSpec.editTemplate().editSpec().addNewContainer().withName(name).endContainer().endSpec().endTemplate();
        }

        spec.withSuspend(config.suspend());
        spec.withConcurrencyPolicy(config.concurrencyPolicy().name());
        config.schedule().ifPresent(spec::withSchedule);
        config.successfulJobsHistoryLimit().ifPresent(spec::withSuccessfulJobsHistoryLimit);
        config.failedJobsHistoryLimit().ifPresent(spec::withFailedJobsHistoryLimit);
        config.startingDeadlineSeconds().ifPresent(spec::withStartingDeadlineSeconds);
        config.timeZone().ifPresent(spec::withTimeZone);

        jobTemplateSpec.withCompletionMode(config.completionMode().name());
        jobTemplateSpec.editTemplate().editSpec().withRestartPolicy(config.restartPolicy().name()).endSpec().endTemplate();
        config.parallelism().ifPresent(jobTemplateSpec::withParallelism);
        config.completions().ifPresent(jobTemplateSpec::withCompletions);
        config.backoffLimit().ifPresent(jobTemplateSpec::withBackoffLimit);
        config.activeDeadlineSeconds().ifPresent(jobTemplateSpec::withActiveDeadlineSeconds);
        config.ttlSecondsAfterFinished().ifPresent(jobTemplateSpec::withTtlSecondsAfterFinished);

        jobTemplateSpec.endSpec().endJobTemplate();
        spec.endSpec();
    }

    private CronJobBuilder createCronJobResource() {
        return new CronJobBuilder().withNewMetadata().withName(name).endMetadata();
    }

    private Function<HasMetadata, CronJobBuilder> replaceExistingCronJobResource(KubernetesListFluent<?> list) {
        return metadata -> {
            list.removeFromItems(metadata);
            return new CronJobBuilder((CronJob) metadata);
        };
    }

    private boolean containsContainerWithName(CronJobFluent<?>.SpecNested<CronJobBuilder> spec) {
        var jobTemplate = spec.buildJobTemplate();
        if (jobTemplate == null
                || jobTemplate.getSpec() == null
                || jobTemplate.getSpec().getTemplate() == null
                || jobTemplate.getSpec().getTemplate().getSpec() == null) {
            return false;
        }

        List<Container> containers = jobTemplate.getSpec().getTemplate().getSpec().getContainers();
        return containers == null || containers.stream().anyMatch(c -> name.equals(c.getName()));
    }
}
