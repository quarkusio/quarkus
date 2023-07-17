package io.quarkus.jackson.runtime;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;
import io.quarkus.jackson.ObjectMapperCustomizer;

@ApplicationScoped
public class ObjectMapperProducer {

    @DefaultBean
    @Singleton
    @Produces
    public ObjectMapper objectMapper(@All List<ObjectMapperCustomizer> customizers,
            JacksonBuildTimeConfig jacksonBuildTimeConfig, JacksonSupport jacksonSupport) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (!jacksonBuildTimeConfig.failOnUnknownProperties) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        if (!jacksonBuildTimeConfig.failOnEmptyBeans) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
        if (!jacksonBuildTimeConfig.writeDatesAsTimestamps) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        if (!jacksonBuildTimeConfig.writeDurationsAsTimestamps) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        }
        if (jacksonBuildTimeConfig.acceptCaseInsensitiveEnums) {
            objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        }
        JsonInclude.Include serializationInclusion = jacksonBuildTimeConfig.serializationInclusion.orElse(null);
        if (serializationInclusion != null) {
            objectMapper.setSerializationInclusion(serializationInclusion);
        }
        ZoneId zoneId = jacksonBuildTimeConfig.timezone.orElse(null);
        if ((zoneId != null) && !zoneId.getId().equals("UTC")) { // Jackson uses UTC as the default, so let's not reset it
            objectMapper.setTimeZone(TimeZone.getTimeZone(zoneId));
        }
        if (jacksonSupport.configuredNamingStrategy().isPresent()) {
            objectMapper.setPropertyNamingStrategy(jacksonSupport.configuredNamingStrategy().get());
        }
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
