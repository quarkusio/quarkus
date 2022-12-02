package io.quarkus.kafka.client.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

final class ObjectMapperProducer {

    private ObjectMapperProducer() {
    }

    // Try to get the ObjectMapper from Arc but fallback to regular ObjectMapper creation
    // The fallback could be used for example in unit tests where Arc has not been initialized
    static ObjectMapper get() {
        ObjectMapper objectMapper = null;
        ArcContainer container = Arc.container();
        if (container != null) {
            objectMapper = container.instance(ObjectMapper.class).get();
        }
        return objectMapper != null ? objectMapper : new ObjectMapper();
    }
}
