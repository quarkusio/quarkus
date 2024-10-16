package io.quarkus.container.image.openshift.deployment;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.ImageStreamSpecFluent;

public class ApplyDockerImageRepositoryToImageStream extends NamedResourceDecorator<ImageStreamSpecFluent<?>> {

    private String dockerImageRepository;

    public ApplyDockerImageRepositoryToImageStream(String name, String dockerImageRepository) {
        super(name);
        this.dockerImageRepository = dockerImageRepository;
    }

    @Override
    public void andThenVisit(ImageStreamSpecFluent<?> spec, ObjectMeta meta) {
        spec.withDockerImageRepository(dockerImageRepository);
    }
}
