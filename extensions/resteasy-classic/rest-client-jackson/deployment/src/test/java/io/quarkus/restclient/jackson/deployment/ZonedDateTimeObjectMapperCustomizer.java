package io.quarkus.restclient.jackson.deployment;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;

import javax.inject.Singleton;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import io.quarkus.jackson.JsonMapperCustomizer;

@Singleton
public class ZonedDateTimeObjectMapperCustomizer implements JsonMapperCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        JavaTimeModule customDateModule = new JavaTimeModule();
        customDateModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(
                new DateTimeFormatterBuilder().appendInstant(0).toFormatter().withZone(ZoneId.of("Z"))));
        customDateModule.addDeserializer(ZonedDateTime.class, new ZonedDateTimeEuropeLondonDeserializer());
        builder.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(customDateModule);
    }

    public static class ZonedDateTimeEuropeLondonDeserializer extends JsonDeserializer<ZonedDateTime> {

        @Override
        public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            return ZonedDateTime.parse(p.getValueAsString())
                    .withZoneSameInstant(ZoneId.of("Europe/London"));
        }
    }
}
