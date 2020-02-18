package io.quarkus.resteasy.mutiny.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.mutiny.runtime.MultiInvokerProvider;
import io.quarkus.resteasy.mutiny.runtime.MultiProvider;
import io.quarkus.resteasy.mutiny.runtime.MultiRxInvoker;
import io.quarkus.resteasy.mutiny.runtime.MultiRxInvokerImpl;
import io.quarkus.resteasy.mutiny.runtime.UniInvokerProvider;
import io.quarkus.resteasy.mutiny.runtime.UniProvider;
import io.quarkus.resteasy.mutiny.runtime.UniRxInvoker;
import io.quarkus.resteasy.mutiny.runtime.UniRxInvokerImpl;

public class ResteasyMutinyProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.RESTEASY_MUTINY);
    }

    @BuildStep
    public void registerProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProvider) {
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(MultiInvokerProvider.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(MultiProvider.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(MultiRxInvoker.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(MultiRxInvokerImpl.class.getName()));

        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(UniInvokerProvider.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(UniProvider.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(UniRxInvoker.class.getName()));
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(UniRxInvokerImpl.class.getName()));
    }

}
