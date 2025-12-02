package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.knative.serving.v1.RevisionSpecFluent;
import io.fabric8.kubernetes.api.model.ObjectMeta;

public class AddImagePullSecretToRevisionSpecDecorator extends NamedResourceDecorator<RevisionSpecFluent<?>> {

    private final String imagePullSecret;

    public AddImagePullSecretToRevisionSpecDecorator(String name, String imagePullSecret) {
        super(name);
        this.imagePullSecret = imagePullSecret;
    }

    @Override
    public void andThenVisit(RevisionSpecFluent<?> revisionSpec, ObjectMeta resourceMeta) {
        revisionSpec.addNewImagePullSecret(imagePullSecret);
    }
}
