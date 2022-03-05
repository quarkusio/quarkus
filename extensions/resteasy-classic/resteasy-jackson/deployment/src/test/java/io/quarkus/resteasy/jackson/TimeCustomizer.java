package io.quarkus.resteasy.jackson;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import io.quarkus.jackson.JsonMapperCustomizer;

@Singleton
public class TimeCustomizer implements JsonMapperCustomizer {

    public static final String HELLO = "hello";

    @Override
    public void customize(JsonMapper.Builder builder) {
        JavaTimeModule customDateModule = new JavaTimeModule();
        customDateModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(
                new DateTimeFormatterBuilder().appendInstant(0).toFormatter().withZone(ZoneId.of("Z"))));
        builder.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(customDateModule);
    }
}
