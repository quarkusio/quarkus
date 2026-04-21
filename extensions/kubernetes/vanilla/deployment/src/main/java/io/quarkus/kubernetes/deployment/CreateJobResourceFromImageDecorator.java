
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.JOB;
import static io.quarkus.kubernetes.deployment.Constants.JOB_API_VERSION;

import java.util.List;

import io.dekorate.kubernetes.annotation.JobCompletionMode;
import io.dekorate.kubernetes.annotation.JobRestartPolicy;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;

/**
 * Create a new Job resource using an image, the command, and arguments.
 **/
public class CreateJobResourceFromImageDecorator extends BaseAddResourceDecorator<Job, JobBuilder, Void> {

    private static final String DEFAULT_RESTART_POLICY = JobRestartPolicy.OnFailure.name();
    private static final String DEFAULT_COMPLETION_MODE = JobCompletionMode.NonIndexed.name();

    private final String image;
    private final List<String> command;
    private final List<String> arguments;

    public CreateJobResourceFromImageDecorator(String name, String image, List<String> command, List<String> arguments) {
        super(name, JOB, JOB_API_VERSION);
        this.image = image;
        this.command = command;
        this.arguments = arguments;
    }

    @Override
    protected JobBuilder builderWithName(String name) {
        return new JobBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(JobBuilder builder, Void config) {
        builder.editOrNewSpec()
                .withCompletionMode(DEFAULT_COMPLETION_MODE)
                .withNewTemplate()
                .withNewSpec()
                .withRestartPolicy(DEFAULT_RESTART_POLICY)
                .addNewContainer()
                .withName(name())
                .withImage(image)
                .withCommand(command)
                .withArgs(arguments)
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec();
    }
}
