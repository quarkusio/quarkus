package io.quarkus.mutiny.reactive.operators.deployment;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.smallrye.mutiny.streams.Engine;

public class MutinyReactiveStreamsOperatorsProcessor {

    @BuildStep
    public void build(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider
                .produce(new ServiceProviderBuildItem(ReactiveStreamsEngine.class.getName(), Engine.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(ReactiveStreamsFactory.class.getName(),
                ReactiveStreamsFactoryImpl.class.getName()));
    }
}
