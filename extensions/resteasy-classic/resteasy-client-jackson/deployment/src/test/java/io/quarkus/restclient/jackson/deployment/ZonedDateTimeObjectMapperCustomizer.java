package io.quarkus.restclient.jackson.deployment;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;

import jakarta.inject.Singleton;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ext.javatime.ser.ZonedDateTimeSerializer;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class ZonedDateTimeObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public int priority() {
        return MINIMUM_PRIORITY;
    }

    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule customDateModule = new SimpleModule();
        customDateModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(
                new DateTimeFormatterBuilder().appendInstant(0).toFormatter().withZone(ZoneId.of("Z"))));
        customDateModule.addDeserializer(ZonedDateTime.class, new ZonedDateTimeEuropeLondonDeserializer());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(customDateModule);
    }

    public static class ZonedDateTimeEuropeLondonDeserializer extends ValueDeserializer<ZonedDateTime> {

        @Override
        public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JacksonException {
            return ZonedDateTime.parse(p.getValueAsString())
                    .withZoneSameInstant(ZoneId.of("Europe/London"));
        }
    }
}
