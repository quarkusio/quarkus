package io.quarkus.rest.client.reactive.jsonb.deployment;

import static io.quarkus.deployment.Feature.REST_CLIENT_JSONB;

import jakarta.ws.rs.RuntimeType;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.jsonb.common.deployment.ResteasyReactiveJsonbCommonProcessor;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class RestClientReactiveJsonbProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(REST_CLIENT_JSONB));
    }

    @BuildStep
    void additionalProviders(BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        ResteasyReactiveJsonbCommonProcessor.additionalProviders(additionalReaders, additionalWriters,
                RuntimeType.CLIENT);
    }
}
