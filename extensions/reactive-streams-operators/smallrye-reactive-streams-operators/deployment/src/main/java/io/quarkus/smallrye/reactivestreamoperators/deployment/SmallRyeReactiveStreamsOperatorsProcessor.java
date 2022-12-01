package io.quarkus.smallrye.reactivestreamoperators.deployment;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreamsFactory;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.smallrye.reactivestreamsoperators.runtime.ReactiveStreamsOperatorsRecorder;
import io.smallrye.reactive.streams.Engine;

public class SmallRyeReactiveStreamsOperatorsProcessor {

    @BuildStep
    public void build(BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_REACTIVE_STREAMS_OPERATORS));
        serviceProvider.produce(new ServiceProviderBuildItem(ReactiveStreamsEngine.class.getName(), Engine.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(ReactiveStreamsFactory.class.getName(),
                ReactiveStreamsFactoryImpl.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void classLoadingHack(ReactiveStreamsOperatorsRecorder reactiveStreamsOperatorsRecorder) {
        reactiveStreamsOperatorsRecorder.classLoaderHack();
    }

}
