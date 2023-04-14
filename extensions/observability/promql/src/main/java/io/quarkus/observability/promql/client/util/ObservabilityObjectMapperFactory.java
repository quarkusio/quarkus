package io.quarkus.observability.promql.client.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class ObservabilityObjectMapperFactory {
    /**
     * @return Common ObjectMapper supporting parameter names.
     */
    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .registerModule(new ParameterNamesModule())
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .disable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .enable(
                        SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
                .disable(
                        DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
