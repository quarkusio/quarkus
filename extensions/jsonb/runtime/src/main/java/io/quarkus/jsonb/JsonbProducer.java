package io.quarkus.jsonb;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;

@Singleton
public class JsonbProducer {

    @Produces
    @Dependent //JsonbConfig is not thread safe so it must not be made singleton.
    @DefaultBean
    public JsonbConfig jsonbConfig(@All List<JsonbConfigCustomizer> customizers) {
        JsonbConfig jsonbConfig = new JsonbConfig();
        for (JsonbConfigCustomizer customizer : customizers) {
            customizer.customize(jsonbConfig);
        }
        return jsonbConfig;
    }

    @Produces
    @Singleton
    @DefaultBean
    public Jsonb jsonb(JsonbConfig jsonbConfig) {
        return JsonbBuilder.create(jsonbConfig);
    }
}
