package io.quarkus.restclient.jsonb.deployment;

import java.lang.reflect.Type;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.inject.Singleton;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import io.quarkus.jsonb.JsonbConfigCustomizer;

@Singleton
public class ZonedDateTimeJsonbConfigCustomizer implements JsonbConfigCustomizer {

    @Override
    public void customize(JsonbConfig jsonbConfig) {
        jsonbConfig.withDeserializers(new ZonedDateTimeWeirdDeserializer());
    }

    public static class ZonedDateTimeWeirdDeserializer implements JsonbDeserializer<ZonedDateTime> {

        @Override
        public ZonedDateTime deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return ZonedDateTime.of(1988, 11, 17, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        }
    }
}
