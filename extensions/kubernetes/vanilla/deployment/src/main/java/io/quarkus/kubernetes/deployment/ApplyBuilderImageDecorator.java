package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.*;
import io.dekorate.s2i.decorator.AddBuildConfigResourceDecorator;
import io.dekorate.utils.Images;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.SourceBuildStrategyFluent;

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
        String builderRepository = Images.getRepository(image);
        String builderTag = Images.getTag(image);
        String builderName = !builderRepository.contains("/") ? builderRepository
                : builderRepository.substring(builderRepository.lastIndexOf("/") + 1);
        strategy.withNewFrom().withKind("ImageStreamTag").withName(builderName + ":" + builderTag).endFrom();
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddBuildConfigResourceDecorator.class };
    }
}
