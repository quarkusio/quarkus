package io.quarkus.resteasy.reactive.jsonb.deployment;

import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Cookie;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.jsonb.common.deployment.ResteasyReactiveJsonbCommonProcessor;
import io.quarkus.resteasy.reactive.jsonb.runtime.mappers.BuiltinJsonParsingExceptionMapper;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class ResteasyReactiveJsonbProcessor {

    private static final String DEFAULT_JSON_PARSING_EXCEPTION_MAPPER = BuiltinJsonParsingExceptionMapper.class.getName();

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.REST_JSONB));
    }

    @BuildStep
    ServerDefaultProducesHandlerBuildItem jsonDefault() {
        return ServerDefaultProducesHandlerBuildItem.json();
    }

    @BuildStep
    void exceptionMappers(BuildProducer<ExceptionMapperBuildItem> producer) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(DEFAULT_JSON_PARSING_EXCEPTION_MAPPER);
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            // the class is not available, likely due to quarkus.class-loading.removed-resources
            return;
        }

        producer.produce(new ExceptionMapperBuildItem(DEFAULT_JSON_PARSING_EXCEPTION_MAPPER,
                JsonParsingException.class.getName(), Priorities.USER + 100, false));
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
        producer.produce(ReflectiveClassBuildItem.builder(BuiltinJsonParsingExceptionMapper.JsonParsingError.class)
                .reason(getClass().getName()).methods().fields().build());
    }
}
