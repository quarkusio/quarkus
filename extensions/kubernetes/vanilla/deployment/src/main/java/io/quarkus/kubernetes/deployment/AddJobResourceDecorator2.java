
package io.quarkus.kubernetes.deployment;

import java.util.List;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;

/**
 * An alternative implementation for {@link AddJobResourceDecorator} that is not using
 * dependant on user provided configuration. Implementations are meant to be aligned at
 * some point ideally in dekorate.
 **/
public class AddJobResourceDecorator2 extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private static final Boolean DEFAULT_SUSPEND = null; //Unspecified
    private static final Integer DEFAULT_PARALLELISM = null; //Unspecified, it will be treated as 1.
    private static final Integer DEFAULT_COMPLETIONS = null; //Unspecified, it will be treated as 1.
    private static final Integer DEFAULT_BACK_OFF_LIMIT = null; //Unspecified
    private static final Long DEFAULT_ACTIVE_DEADLINE_SECONDS = null; //Unspecified
    private static final Integer DEFAULT_TTL_AFTER_FINISHED = null; //Unspecified;
    private static final String DEFAULT_RESTART_POLICY = "OnFailure";
    private static final String DEFAULT_COMPLETION_MODE = "OnFailure";

    private final String name;
    private final String image;
    private final List<String> command;
    private final List<String> arguments;
    private final Boolean suspend;
    private final Integer parallelism;
    private final Integer completions;
    private final Integer backOffLimit;
    private final Long activeDeadlineSeconds;
    private final Integer ttlSecondsAfterFinished;
    private final String restartPolicy;
    private final String completionMode;

    public AddJobResourceDecorator2(String name, String image, List<String> command, List<String> arguments) {
        this(name, image, command, arguments, DEFAULT_SUSPEND, DEFAULT_PARALLELISM, DEFAULT_COMPLETIONS, DEFAULT_BACK_OFF_LIMIT,
                DEFAULT_ACTIVE_DEADLINE_SECONDS, DEFAULT_TTL_AFTER_FINISHED, DEFAULT_RESTART_POLICY, DEFAULT_COMPLETION_MODE);
    }

    public AddJobResourceDecorator2(String name, String image, List<String> command, List<String> arguments, Boolean suspend,
            Integer parallelism, Integer completions,
            Integer backOffLimit, Long activeDeadlineSeconds, Integer ttlSecondsAfterFinished, String restartPolicy,
            String completionMode) {
        this.name = name;
        this.image = image;
        this.command = command;
        this.arguments = arguments;
        this.suspend = suspend;
        this.parallelism = parallelism;
        this.completions = completions;
        this.backOffLimit = backOffLimit;
        this.activeDeadlineSeconds = activeDeadlineSeconds;
        this.ttlSecondsAfterFinished = ttlSecondsAfterFinished;
        this.restartPolicy = restartPolicy;
        this.completionMode = completionMode;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void visit(KubernetesListBuilder list) {
        if (!contains(list, "batch/v1", "Job", name)) {
            list.addToItems(new JobBuilder()
                    .withNewMetadata()
                    .withName(name)
                    .endMetadata()
                    .withNewSpec()
                    .withSuspend(suspend)
                    .withParallelism(parallelism)
                    .withCompletions(completions)
                    .withBackoffLimit(backOffLimit)
                    .withActiveDeadlineSeconds(activeDeadlineSeconds)
                    .withTtlSecondsAfterFinished(ttlSecondsAfterFinished)
                    .withCompletionMode(completionMode)
                    .withNewTemplate()
                    .withNewSpec()
                    .withRestartPolicy(restartPolicy)
                    .addNewContainer()
                    .withName(name)
                    .withImage(image)
                    .withCommand(command)
                    .withArgs(arguments)
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build());
        }
    }
}
