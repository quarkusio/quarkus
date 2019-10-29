package io.quarkus.smallrye.typeconverters.deployment;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

/**
 * Searches for implementations of the {@link ReactiveTypeConverter} class and register them as SPI. So the result depends
 * of the implementation added by the user in the build classpath (Maven dependencies).
 *
 * Note that if none are found, nothing is added - so declaring this augmentation is quite useless in this case.
 */
public class SmallRyeReactiveTypeConvertersProcessor {

    private static final DotName REACTIVE_TYPE_CONVERTER = DotName.createSimple(ReactiveTypeConverter.class.getName());

    @BuildStep
    public void build(BuildProducer<ServiceProviderBuildItem> serviceProvider, BuildProducer<FeatureBuildItem> feature,
            CombinedIndexBuildItem indexBuildItem) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_TYPE_CONVERTERS));
        Collection<ClassInfo> implementors = indexBuildItem.getIndex().getAllKnownImplementors(REACTIVE_TYPE_CONVERTER);

        implementors.forEach(info -> serviceProvider
                .produce(new ServiceProviderBuildItem(REACTIVE_TYPE_CONVERTER.toString(), info.toString())));
    }

}
