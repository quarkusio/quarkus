package io.quarkus.restclient.jsonb.deployment;

import java.lang.reflect.Type;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.inject.Singleton;
import javax.json.bind.JsonbConfig;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;

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
