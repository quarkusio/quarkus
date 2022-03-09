package io.quarkus.jackson.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.quarkus.arc.DefaultBean;
import io.quarkus.jackson.ObjectMapperCustomizer;

@ApplicationScoped
public class ObjectMapperProducer {

    @DefaultBean
    @Singleton
    @Produces
    public ObjectMapper objectMapper(Instance<ObjectMapperCustomizer> customizers,
            JacksonBuildTimeConfig jacksonBuildTimeConfig) {
        ObjectMapper objectMapper = new ObjectMapper();

        // These are done first so that the other boolean properties will take precedence over them
        jacksonBuildTimeConfig.serialization.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> Map.entry(SerializationFeature.valueOf(entry.getKey().toUpperCase().replace('-', '_')),
                        entry.getValue()))
                .forEach(entry -> objectMapper.configure(entry.getKey(), entry.getValue()));

        jacksonBuildTimeConfig.deserialization.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> Map.entry(DeserializationFeature.valueOf(entry.getKey().toUpperCase().replace('-', '_')),
                        entry.getValue()))
                .forEach(entry -> objectMapper.configure(entry.getKey(), entry.getValue()));

        jacksonBuildTimeConfig.mapper.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> Map.entry(MapperFeature.valueOf(entry.getKey().toUpperCase().replace('-', '_')),
                        entry.getValue()))
                .forEach(entry -> objectMapper.configure(entry.getKey(), entry.getValue()));

        jacksonBuildTimeConfig.parser.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> Map.entry(JsonParser.Feature.valueOf(entry.getKey().toUpperCase().replace('-', '_')),
                        entry.getValue()))
                .forEach(entry -> objectMapper.configure(entry.getKey(), entry.getValue()));

        jacksonBuildTimeConfig.generator.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> Map.entry(JsonGenerator.Feature.valueOf(entry.getKey().toUpperCase().replace('-', '_')),
                        entry.getValue()))
                .forEach(entry -> objectMapper.configure(entry.getKey(), entry.getValue()));

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                jacksonBuildTimeConfig.failOnUnknownProperties);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, jacksonBuildTimeConfig.failOnEmptyBeans);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                jacksonBuildTimeConfig.writeDatesAsTimestamps);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS,
                jacksonBuildTimeConfig.acceptCaseInsensitiveEnums);

        jacksonBuildTimeConfig.serializationInclusion.ifPresent(objectMapper::setSerializationInclusion);

        jacksonBuildTimeConfig.timezone
                .filter(zoneId -> !zoneId.getId().equals("UTC")) // Jackson uses UTC as the default, so let's not reset it
                .map(TimeZone::getTimeZone)
                .ifPresent(objectMapper::setTimeZone);

        sortCustomizersInDescendingPriorityOrder(customizers)
                .forEach(customizer -> customizer.customize(objectMapper));

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
