package io.quarkus.container.image.openshift.deployment;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.BuildOutputFluent;

/**
 * Allows conformity between the configuration and output resources when it comes to the name of the image stream and
 * the image stream tag for local openshift builds.
 */
public class ApplyImageStreamNameToBuildConfigDecorator extends NamedResourceDecorator<BuildOutputFluent<?>> {

    final String imageStreamName;
    final String imageStreamTag;

    public ApplyImageStreamNameToBuildConfigDecorator(String buildConfigName, String imageStreamName, String imageStreamTag) {
        super(buildConfigName);
        this.imageStreamName = imageStreamName;
        this.imageStreamTag = imageStreamTag;
    }

    public void andThenVisit(BuildOutputFluent<?> output, ObjectMeta objectMeta) {
        output.withNewTo()
                .withKind("ImageStreamTag")
                .withName(String.format("%s:%s", imageStreamName, imageStreamTag))
                .endTo();
    }
}
