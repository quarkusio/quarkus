package io.quarkus.jackson.runtime;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

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
            JacksonConfigSupport jacksonConfigSupport) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (!jacksonConfigSupport.isFailOnUnknownProperties()) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        if (!jacksonConfigSupport.isWriteDatesAsTimestamps()) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        if (jacksonConfigSupport.isAcceptCaseInsensitiveEnums()) {
            objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        }
        ZoneId zoneId = jacksonConfigSupport.getTimeZone();
        if ((zoneId != null) && !zoneId.getId().equals("UTC")) { // Jackson uses UTC as the default, so let's not reset it
            objectMapper.setTimeZone(TimeZone.getTimeZone(zoneId));
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
