package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT_INTERNAL_REGISTRY_PROJECT;

import java.util.Optional;

import io.dekorate.kubernetes.decorator.*;
import io.dekorate.s2i.decorator.AddBuildConfigResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.SourceBuildStrategyFluent;
import io.quarkus.container.spi.ImageReference;

public class ApplyBuilderImageDecorator extends NamedResourceDecorator<SourceBuildStrategyFluent<?>> {

    private final String image;

    public ApplyBuilderImageDecorator(String image) {
        this(ANY, image);
    }

    public ApplyBuilderImageDecorator(String name, String image) {
        super(name);
        this.image = image;
    }

    @Override
    public void andThenVisit(SourceBuildStrategyFluent<?> strategy, ObjectMeta meta) {
        ImageReference imageRef = ImageReference.parse(image);

        String builderRepository = imageRef.getRepository();
        String builderTag = imageRef.getTag();
        String builderName = !builderRepository.contains("/") ? builderRepository
                : builderRepository.substring(builderRepository.lastIndexOf("/") + 1);
        Optional<String> builderGroup = Optional.of(builderRepository)
                .filter(s -> s.contains("/"))
                .map(s -> s.substring(0, s.indexOf("/")));

        boolean usesInternalRegistry = imageRef.getRegistry()
                .filter(registry -> registry.contains(OPENSHIFT_INTERNAL_REGISTRY_PROJECT)).isPresent();
        strategy.withNewFrom()
                .withKind("ImageStreamTag")
                .withName(builderName + ":" + builderTag)
                .withNamespace(builderGroup.filter(g -> usesInternalRegistry).orElse(null))
                .endFrom();
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddBuildConfigResourceDecorator.class };
    }
}
