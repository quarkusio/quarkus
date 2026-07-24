package io.quarkus.jackson.runtime;

import java.time.ZoneId;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Singleton
public class ConfigurationCustomizer implements JsonMapperBuilderCustomizer {
    @Inject
    JacksonBuildTimeConfig jacksonBuildTimeConfig;

    @Inject
    JacksonSupport jacksonSupport;

    @Override
    public void customize(JsonMapper.Builder builder) {
        if (jacksonBuildTimeConfig.failOnUnknownProperties()) {
            builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        if (jacksonBuildTimeConfig.failOnNullForPrimitives()) {
            builder.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        } else {
            builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        }
        if (jacksonBuildTimeConfig.defaultViewInclusion()) {
            builder.enable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        }
        if (jacksonBuildTimeConfig.failOnTrailingTokens()) {
            builder.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        } else {
            builder.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        }
        if (jacksonBuildTimeConfig.failOnEmptyBeans()) {
            builder.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
        if (jacksonBuildTimeConfig.writeDatesAsTimestamps()) {
            builder.enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        if (jacksonBuildTimeConfig.writeDurationsAsTimestamps()) {
            builder.enable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        }
        if (jacksonBuildTimeConfig.useGettersAsSetters()) {
            builder.enable(MapperFeature.USE_GETTERS_AS_SETTERS);
        }
        if (jacksonBuildTimeConfig.acceptCaseInsensitiveEnums()) {
            builder.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        }
        JsonInclude.Include serializationInclusion = jacksonBuildTimeConfig.serializationInclusion().orElse(null);
        if (serializationInclusion != null) {
            builder.changeDefaultPropertyInclusion(new UnaryOperator<>() {
                @Override
                public JsonInclude.Value apply(JsonInclude.Value value) {
                    return value.withValueInclusion(serializationInclusion);
                }
            });
        }
        ZoneId zoneId = jacksonBuildTimeConfig.timezone();
        if (!zoneId.getId().equals("UTC")) { // Jackson uses UTC as the default, so let's not reset it
            builder.defaultTimeZone(TimeZone.getTimeZone(zoneId));
        }
        if (jacksonSupport.configuredNamingStrategy().isPresent()) {
            builder.propertyNamingStrategy(jacksonSupport.configuredNamingStrategy().get());
        }
    }

    @Override
    public int priority() {
        // we return the maximum possible priority to make sure these
        // settings are always applied first, before any other customizers.
        return JsonMapperBuilderCustomizer.MAXIMUM_PRIORITY;
    }
}
