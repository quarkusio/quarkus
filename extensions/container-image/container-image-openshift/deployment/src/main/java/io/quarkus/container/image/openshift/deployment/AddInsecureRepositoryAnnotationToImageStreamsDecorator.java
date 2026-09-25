package io.quarkus.container.image.openshift.deployment;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.s2i.decorator.AddBuilderImageStreamResourceDecorator;
import io.dekorate.s2i.decorator.AddDockerImageStreamResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaFluent;

/**
 * Marks the generated image streams as insecure repositories ({@code openshift.io/image.insecureRepository}), which
 * OpenShift honours when it imports their images through {@code spec.dockerImageRepository}
 * ({@code quarkus.container-image.insecure}).
 */
public class AddInsecureRepositoryAnnotationToImageStreamsDecorator extends NamedResourceDecorator<ObjectMetaFluent<?>> {

    private static final String IMAGE_STREAM = "ImageStream";
    private static final String INSECURE_REPOSITORY_ANNOTATION = "openshift.io/image.insecureRepository";

    public AddInsecureRepositoryAnnotationToImageStreamsDecorator() {
        super(IMAGE_STREAM, ANY);
    }

    @Override
    public void andThenVisit(ObjectMetaFluent<?> metadata, ObjectMeta resourceMeta) {
        metadata.addToAnnotations(INSECURE_REPOSITORY_ANNOTATION, "true");
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddBuilderImageStreamResourceDecorator.class, AddDockerImageStreamResourceDecorator.class,
                ApplyDockerImageRepositoryToImageStream.class };
    }
}
