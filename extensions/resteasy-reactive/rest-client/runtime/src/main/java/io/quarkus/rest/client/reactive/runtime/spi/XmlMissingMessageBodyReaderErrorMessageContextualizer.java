package io.quarkus.rest.client.reactive.runtime.spi;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.client.spi.MissingMessageBodyReaderErrorMessageContextualizer;

public class XmlMissingMessageBodyReaderErrorMessageContextualizer implements
        MissingMessageBodyReaderErrorMessageContextualizer {
    @Override
    public String provideContextMessage(Input input) {
        if ((input.mediaType() != null) && input.mediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
            return "Consider adding the 'quarkus-rest-client-jaxb' extension";
        }
        return null;
    }
}
