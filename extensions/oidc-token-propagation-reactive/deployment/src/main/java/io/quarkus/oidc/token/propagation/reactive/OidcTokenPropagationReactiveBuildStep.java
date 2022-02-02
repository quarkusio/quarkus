package io.quarkus.oidc.token.propagation.reactive;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class OidcTokenPropagationReactiveBuildStep {

    @BuildStep(onlyIf = IsEnabled.class)
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.OIDC_TOKEN_PROPAGATION_REACTIVE);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(AccessTokenRequestReactiveFilter.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, AccessTokenRequestReactiveFilter.class));
        additionalIndexedClassesBuildItem
                .produce(new AdditionalIndexedClassesBuildItem(AccessTokenRequestReactiveFilter.class.getName()));

    }

    public static class IsEnabled implements BooleanSupplier {
        OidcTokenPropagationReactiveBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
