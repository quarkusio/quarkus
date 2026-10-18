package io.quarkus.jackson.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import io.quarkus.jackson.JsonFactoryBuilderCustomizer;
import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.databind.json.JsonMapper;

@ApplicationScoped
public class JsonMapperProducer {

    @DefaultBean
    @Singleton
    @Produces
    public JsonMapper jsonMapper(Instance<JsonMapperBuilderCustomizer> customizers,
            Instance<JsonFactoryBuilderCustomizer> factoryCustomizers) {
        JsonMapper.Builder builder = JsonMapper.builder(createJsonFactory(factoryCustomizers));
        List<JsonMapperBuilderCustomizer> sortedCustomizers = sortCustomizersInDescendingPriorityOrder(customizers);
        for (JsonMapperBuilderCustomizer customizer : sortedCustomizers) {
            customizer.customize(builder);
        }
        return builder.build();
    }

    private List<JsonMapperBuilderCustomizer> sortCustomizersInDescendingPriorityOrder(
            Iterable<JsonMapperBuilderCustomizer> customizers) {
        List<JsonMapperBuilderCustomizer> sortedCustomizers = new ArrayList<>();
        for (JsonMapperBuilderCustomizer customizer : customizers) {
            sortedCustomizers.add(customizer);
        }
        Collections.sort(sortedCustomizers);
        return sortedCustomizers;
    }

    private static JsonFactory createJsonFactory(Iterable<JsonFactoryBuilderCustomizer> customizers) {
        JsonFactoryBuilder factoryBuilder = JsonFactory.builder();
        List<JsonFactoryBuilderCustomizer> sortedCustomizers = new ArrayList<>();
        for (JsonFactoryBuilderCustomizer customizer : customizers) {
            sortedCustomizers.add(customizer);
        }
        Collections.sort(sortedCustomizers);
        for (JsonFactoryBuilderCustomizer customizer : sortedCustomizers) {
            customizer.customize(factoryBuilder);
        }
        return factoryBuilder.build();
    }
}
