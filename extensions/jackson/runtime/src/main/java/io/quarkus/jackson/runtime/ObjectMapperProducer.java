package io.quarkus.jackson.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.DefaultBean;
import io.quarkus.jackson.ObjectMapperCustomizer;

@ApplicationScoped
public class ObjectMapperProducer {

    @DefaultBean
    @Singleton
    @Produces
    public ObjectMapper objectMapper(Instance<ObjectMapperCustomizer> customizers,
            JacksonConfigSupport jacksonConfigSupport) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (!jacksonConfigSupport.isFailOnUnknownProperties()) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        List<ObjectMapperCustomizer> sortedCustomizers = sortCustomizersInDescendingPriorityOrder(customizers);
        for (ObjectMapperCustomizer customizer : sortedCustomizers) {
            customizer.customize(objectMapper);
        }
        return objectMapper;
    }

    private List<ObjectMapperCustomizer> sortCustomizersInDescendingPriorityOrder(
            Instance<ObjectMapperCustomizer> customizers) {
        List<ObjectMapperCustomizer> sortedCustomizers = new ArrayList<>();
        for (ObjectMapperCustomizer customizer : customizers) {
            sortedCustomizers.add(customizer);
        }
        Collections.sort(sortedCustomizers);
        return sortedCustomizers;
    }
}
