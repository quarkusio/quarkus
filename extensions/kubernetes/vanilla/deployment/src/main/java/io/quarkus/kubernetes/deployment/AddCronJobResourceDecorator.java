
package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobBuilder;

public class AddCronJobResourceDecorator extends BaseAddDeploymentResourceDecorator<CronJob, CronJobBuilder, CronJobConfig> {
    public AddCronJobResourceDecorator(String name, CronJobConfig config, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.CronJob, config, toRemove);
    }

    @Override
    protected CronJobBuilder builderWithName(String name) {
        return new CronJobBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(CronJobBuilder builder, CronJobConfig config) {
        final var spec = builder.editOrNewSpec();

        var jobTemplateSpec = spec.editOrNewJobTemplate().editOrNewSpec();

        // match labels for selector
        // for some reason, tests want null selector if not explicitly set
        if (jobTemplateSpec.hasSelector()) {
            initSelectorMatchLabels(jobTemplateSpec.editSelector())
                    .endSelector();
        }

        // ensure defaults on template spec
        podSpecDefaults(jobTemplateSpec.editOrNewTemplate().editOrNewSpec())
                .endSpec().endTemplate();

        spec.withSuspend(config.suspend());
        spec.withConcurrencyPolicy(config.concurrencyPolicy().name());

        config.schedule().ifPresent(spec::withSchedule);
        // check that we end up with a schedule, either from existing resource or configuration
        final var schedule = spec.getSchedule();
        if (schedule == null || schedule.isBlank()) {
            throw new IllegalArgumentException(
                    "When generating a CronJob resource, you need to specify a schedule CRON expression.");
        }
        config.successfulJobsHistoryLimit().ifPresent(spec::withSuccessfulJobsHistoryLimit);
        config.failedJobsHistoryLimit().ifPresent(spec::withFailedJobsHistoryLimit);
        config.startingDeadlineSeconds().ifPresent(spec::withStartingDeadlineSeconds);
        config.timeZone().ifPresent(spec::withTimeZone);

        // init job template from config
        initFromConfig(jobTemplateSpec, config);

        jobTemplateSpec.endSpec().endJobTemplate();
        spec.endSpec();
    }
}
