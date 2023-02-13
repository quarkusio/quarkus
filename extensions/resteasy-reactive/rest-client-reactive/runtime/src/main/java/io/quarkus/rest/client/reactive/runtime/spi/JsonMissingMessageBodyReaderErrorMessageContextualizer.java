package io.quarkus.rest.client.reactive.runtime.spi;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.client.spi.MissingMessageBodyReaderErrorMessageContextualizer;

public class JsonMissingMessageBodyReaderErrorMessageContextualizer implements
        MissingMessageBodyReaderErrorMessageContextualizer {
    @Override
    public String provideContextMessage(Input input) {
        if ((input.mediaType() != null) && input.mediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
            return "Consider adding one the 'quarkus-rest-client-reactive-jackson' or 'quarkus-rest-client-reactive-jsonb' extensions";
        }
        return null;
    }
}
