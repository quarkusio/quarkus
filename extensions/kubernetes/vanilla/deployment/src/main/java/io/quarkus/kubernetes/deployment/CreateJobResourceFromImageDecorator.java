
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.JOB;
import static io.quarkus.kubernetes.deployment.Constants.JOB_API_VERSION;

import java.util.List;

import io.dekorate.kubernetes.annotation.JobCompletionMode;
import io.dekorate.kubernetes.annotation.JobRestartPolicy;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;

/**
 * Create a new Job resource using an image, the command, and arguments.
 **/
public class CreateJobResourceFromImageDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private static final String DEFAULT_RESTART_POLICY = JobRestartPolicy.OnFailure.name();
    private static final String DEFAULT_COMPLETION_MODE = JobCompletionMode.NonIndexed.name();

    private final String name;
    private final String image;
    private final List<String> command;
    private final List<String> arguments;

    public CreateJobResourceFromImageDecorator(String name, String image, List<String> command,
            List<String> arguments) {
        this.name = name;
        this.image = image;
        this.command = command;
        this.arguments = arguments;
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        if (!contains(list, JOB_API_VERSION, JOB, name)) {
            list.addToItems(new JobBuilder().withNewMetadata().withName(name).endMetadata().withNewSpec()
                    .withCompletionMode(DEFAULT_COMPLETION_MODE).withNewTemplate().withNewSpec()
                    .withRestartPolicy(DEFAULT_RESTART_POLICY).addNewContainer().withName(name).withImage(image)
                    .withCommand(command).withArgs(arguments).endContainer().endSpec().endTemplate().endSpec().build());
        }
    }
}
