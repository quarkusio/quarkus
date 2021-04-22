package io.quarkus.resteasy.mutiny.common.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.mutiny.common.runtime.MultiInvokerProvider;
import io.quarkus.resteasy.mutiny.common.runtime.MultiProvider;
import io.quarkus.resteasy.mutiny.common.runtime.MultiRxInvoker;
import io.quarkus.resteasy.mutiny.common.runtime.MultiRxInvokerImpl;
import io.quarkus.resteasy.mutiny.common.runtime.UniInvokerProvider;
import io.quarkus.resteasy.mutiny.common.runtime.UniProvider;
import io.quarkus.resteasy.mutiny.common.runtime.UniRxInvoker;
import io.quarkus.resteasy.mutiny.common.runtime.UniRxInvokerImpl;

public class ResteasyMutinyCommonProcessor {

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
