package io.quarkus.apicurio.registry.config;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import jakarta.inject.Singleton;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Replaces Apicurio Registry's {@code JacksonDateTimeCustomizer} which uses Jackson 2.
 * <p>
 * TODO: remove when JacksonDateTimeCustomizer has been ported to Jackson 3
 */
@Singleton
public class ApicurioRegistryDateTimeCustomizer implements JsonMapperBuilderCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .defaultDateFormat(dateFormat);
    }
}
