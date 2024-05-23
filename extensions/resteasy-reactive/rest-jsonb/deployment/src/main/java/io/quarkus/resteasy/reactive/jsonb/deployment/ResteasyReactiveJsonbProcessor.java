package io.quarkus.resteasy.reactive.jsonb.deployment;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Cookie;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.jsonb.common.deployment.ResteasyReactiveJsonbCommonProcessor;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class ResteasyReactiveJsonbProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.REST_JSONB));
    }

    @BuildStep
    ServerDefaultProducesHandlerBuildItem jsonDefault() {
        return ServerDefaultProducesHandlerBuildItem.json();
    }

    @BuildStep
    void additionalProviders(BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        ResteasyReactiveJsonbCommonProcessor.additionalProviders(additionalReaders, additionalWriters,
                RuntimeType.SERVER);
    }

    @BuildStep
    void reflection(BuildProducer<ReflectiveClassBuildItem> producer) {
        producer.produce(ReflectiveClassBuildItem.builder(Cookie.class).reason(getClass().getName()).methods().build());
    }
}
