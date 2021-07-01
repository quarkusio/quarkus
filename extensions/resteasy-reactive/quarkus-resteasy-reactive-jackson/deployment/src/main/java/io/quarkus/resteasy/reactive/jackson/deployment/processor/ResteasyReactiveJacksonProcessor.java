package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.Priorities;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.jackson.runtime.mappers.DefaultMismatchedInputException;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.BasicServerJacksonMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.FullyFeaturedServerJacksonMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.ServerJacksonMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResteasyReactiveJacksonProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_REACTIVE_JACKSON));
    }

    @BuildStep
    ServerDefaultProducesHandlerBuildItem jsonDefault() {
        return ServerDefaultProducesHandlerBuildItem.json();
    }

    @BuildStep
    ResteasyReactiveJacksonProviderDefinedBuildItem jacksonRegistered() {
        return new ResteasyReactiveJacksonProviderDefinedBuildItem();
    }

    @BuildStep
    void additionalProviders(List<JacksonFeatureBuildItem> jacksonFeatureBuildItems,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters,
            BuildProducer<ExceptionMapperBuildItem> exceptionMappers) {
        boolean applicationNeedsSpecialJacksonFeatures = jacksonFeatureBuildItems.isEmpty();
        // make these beans to they can get instantiated with the Quarkus CDI configured ObjectMapper object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ServerJacksonMessageBodyReader.class.getName())
                .addBeanClass(getJacksonMessageBodyWriter(applicationNeedsSpecialJacksonFeatures))
                .setUnremovable().build());

        additionalReaders
                .produce(new MessageBodyReaderBuildItem(ServerJacksonMessageBodyReader.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalReaders
                .produce(new MessageBodyReaderBuildItem(VertxJsonArrayMessageBodyReader.class.getName(),
                        JsonArray.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalReaders
                .produce(new MessageBodyReaderBuildItem(VertxJsonObjectMessageBodyReader.class.getName(),
                        JsonObject.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(getJacksonMessageBodyWriter(applicationNeedsSpecialJacksonFeatures),
                        Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(VertxJsonArrayMessageBodyWriter.class.getName(),
                        JsonArray.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(VertxJsonObjectMessageBodyWriter.class.getName(),
                        JsonObject.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));

        exceptionMappers.produce(new ExceptionMapperBuildItem(DefaultMismatchedInputException.class.getName(),
                MismatchedInputException.class.getName(), Priorities.USER + 100, false));
    }

    private String getJacksonMessageBodyWriter(boolean applicationNeedsSpecialJacksonFeatures) {
        return applicationNeedsSpecialJacksonFeatures ? BasicServerJacksonMessageBodyWriter.class.getName()
                : FullyFeaturedServerJacksonMessageBodyWriter.class.getName();
    }
}
