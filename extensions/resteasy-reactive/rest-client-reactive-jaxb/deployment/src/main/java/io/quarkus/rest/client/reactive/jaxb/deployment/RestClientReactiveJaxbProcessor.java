package io.quarkus.rest.client.reactive.jaxb.deployment;

import static io.quarkus.deployment.Feature.REST_CLIENT_REACTIVE_JAXB;

import java.util.List;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.rest.client.reactive.jaxb.runtime.ClientJaxbMessageBodyReader;
import io.quarkus.rest.client.reactive.jaxb.runtime.ClientMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class RestClientReactiveJaxbProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(REST_CLIENT_REACTIVE_JAXB));
    }

    @BuildStep
    void additionalProviders(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        // make these beans to they can get instantiated with the Quarkus CDI
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ClientJaxbMessageBodyReader.class.getName())
                .addBeanClass(ClientMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders
                .produce(new MessageBodyReaderBuildItem(ClientJaxbMessageBodyReader.class.getName(), Object.class.getName(),
                        List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML), RuntimeType.CLIENT, true, Priorities.USER));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(ClientMessageBodyWriter.class.getName(), Object.class.getName(),
                        List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML), RuntimeType.CLIENT, true, Priorities.USER));
    }
}
