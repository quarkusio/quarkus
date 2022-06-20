package io.quarkus.resteasy.reactive.jaxb.common.deployment;

import java.util.List;

import javax.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.reactive.jaxb.common.runtime.serialisers.JaxbMessageBodyReader;
import io.quarkus.resteasy.reactive.jaxb.common.runtime.serialisers.JaxbMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class ResteasyReactiveJaxbCommonProcessor {

    @BuildStep
    void additionalProviders(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        // make these beans to they can get instantiated with the Quarkus CDI
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JaxbMessageBodyReader.class.getName())
                .addBeanClass(JaxbMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders.produce(new MessageBodyReaderBuildItem(JaxbMessageBodyReader.class.getName(), Object.class.getName(),
                List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML)));
        additionalWriters.produce(new MessageBodyWriterBuildItem(JaxbMessageBodyWriter.class.getName(), Object.class.getName(),
                List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML)));
    }
}
