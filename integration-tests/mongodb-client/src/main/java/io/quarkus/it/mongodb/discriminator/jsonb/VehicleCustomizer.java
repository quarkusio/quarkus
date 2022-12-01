package io.quarkus.it.mongodb.discriminator.jsonb;

import javax.inject.Singleton;
import javax.json.bind.JsonbConfig;

import io.quarkus.jsonb.JsonbConfigCustomizer;

@Singleton
public class VehicleCustomizer implements JsonbConfigCustomizer {
    @Override
    public void customize(JsonbConfig jsonbConfig) {
        jsonbConfig.withDeserializers(new VehicleDeserializer());
    }
}
