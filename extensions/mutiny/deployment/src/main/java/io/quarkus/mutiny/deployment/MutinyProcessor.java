package io.quarkus.mutiny.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.mutiny.converters.MultiConverter;
import io.quarkus.mutiny.converters.UniConverter;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

public class MutinyProcessor {

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(FeatureBuildItem.MUTINY);
    }

    private static final DotName REACTIVE_TYPE_CONVERTER = DotName.createSimple(ReactiveTypeConverter.class.getName());

    @BuildStep
    public void registerReactiveConverters(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(new ServiceProviderBuildItem(REACTIVE_TYPE_CONVERTER.toString(),
                UniConverter.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(REACTIVE_TYPE_CONVERTER.toString(),
                MultiConverter.class.getName()));
    }
}
