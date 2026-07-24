package io.quarkus.restclient.jackson.deployment;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;

import jakarta.inject.Singleton;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.ser.ZonedDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Singleton
public class ZonedDateTimeObjectMapperCustomizer implements JsonMapperBuilderCustomizer {

    @Override
    public int priority() {
        return MINIMUM_PRIORITY;
    }

    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleModule customDateModule = new SimpleModule();
        customDateModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(
                new DateTimeFormatterBuilder().appendInstant(0).toFormatter().withZone(ZoneId.of("Z"))));
        customDateModule.addDeserializer(ZonedDateTime.class, new ZonedDateTimeEuropeLondonDeserializer());
        builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).addModule(customDateModule);
    }

    public static class ZonedDateTimeEuropeLondonDeserializer extends ValueDeserializer<ZonedDateTime> {

        @Override
        public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
            return ZonedDateTime.parse(p.getValueAsString())
                    .withZoneSameInstant(ZoneId.of("Europe/London"));
        }
    }
}
