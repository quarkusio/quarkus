package io.quarkus.container.image.openshift.deployment;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.BuildOutputFluent;

public class ApplyDockerImageOutputToBuildConfigDecorator extends NamedResourceDecorator<BuildOutputFluent<?>> {

    String image;
    String pushSecret;

    public ApplyDockerImageOutputToBuildConfigDecorator(String name, String image, String pushSecret) {
        super(name);
        this.image = image;
        this.pushSecret = pushSecret;
    }

    public void andThenVisit(BuildOutputFluent<?> output, ObjectMeta objectMeta) {
        output.withPushSecret(new LocalObjectReference(pushSecret));
        output.withNewTo()
                .withKind("DockerImage")
                .withName(image)
                .endTo();
    }
}
