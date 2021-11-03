package io.quarkus.resteasy.reactive.jaxb.deployment.processor;

import java.util.Collections;

import javax.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.jaxb.runtime.serialisers.JaxbMessageBodyReader;
import io.quarkus.resteasy.reactive.jaxb.runtime.serialisers.JaxbMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class ResteasyReactiveJaxbProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_REACTIVE_JAXB));
    }

    @BuildStep
    void additionalProviders(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        // make these beans to they can get instantiated with the Quarkus CDI configured Jsonb object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JaxbMessageBodyReader.class.getName())
                .addBeanClass(JaxbMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders.produce(new MessageBodyReaderBuildItem(JaxbMessageBodyReader.class.getName(), Object.class.getName(),
                Collections.singletonList(MediaType.APPLICATION_XML)));
        additionalWriters.produce(new MessageBodyWriterBuildItem(JaxbMessageBodyWriter.class.getName(), Object.class.getName(),
                Collections.singletonList(MediaType.APPLICATION_XML)));
    }
}
