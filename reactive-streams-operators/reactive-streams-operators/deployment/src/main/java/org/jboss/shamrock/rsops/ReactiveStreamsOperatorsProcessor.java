package org.jboss.shamrock.rsops;

import io.smallrye.reactive.streams.Engine;
import org.eclipse.microprofile.reactive.streams.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.substrate.ServiceProviderBuildItem;

public class ReactiveStreamsOperatorsProcessor {

    @BuildStep
    public void build(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(new ServiceProviderBuildItem(ReactiveStreamsEngine.class.getName(), Engine.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(ReactiveStreamsFactory.class.getName(), ReactiveStreamsFactoryImpl.class.getName()));
    }

}
