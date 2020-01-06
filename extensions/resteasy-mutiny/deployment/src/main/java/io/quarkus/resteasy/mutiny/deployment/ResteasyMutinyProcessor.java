package io.quarkus.resteasy.mutiny.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.mutiny.runtime.*;

public class ResteasyMutinyProcessor {

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem("resteasy-mutiny");
    }

    @BuildStep
    public void registerProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProvider) {
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(MultiInvokerProvider.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(UniInvokerProvider.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(MultiRxInvoker.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(UniRxInvoker.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(MultiRxInvokerImpl.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(UniRxInvokerImpl.class.getName()));
    }
}
