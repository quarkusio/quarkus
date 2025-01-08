package io.quarkus.jackson.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.DefaultBean;
import io.quarkus.jackson.ObjectMapperCustomizer;

@ApplicationScoped
public class ObjectMapperProducer {
    @DefaultBean
    @Singleton
    @Produces
    public ObjectMapper objectMapper(Instance<ObjectMapperCustomizer> customizers) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ObjectMapperCustomizer> sortedCustomizers = sortCustomizersInDescendingPriorityOrder(customizers);
        for (ObjectMapperCustomizer customizer : sortedCustomizers) {
            customizer.customize(objectMapper);
        }
        return objectMapper;
    }

    private List<ObjectMapperCustomizer> sortCustomizersInDescendingPriorityOrder(
            Iterable<ObjectMapperCustomizer> customizers) {
        List<ObjectMapperCustomizer> sortedCustomizers = new ArrayList<>();
        for (ObjectMapperCustomizer customizer : customizers) {
            sortedCustomizers.add(customizer);
        }
        Collections.sort(sortedCustomizers);
        return sortedCustomizers;
    }
}
