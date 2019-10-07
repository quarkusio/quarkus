package io.quarkus.jsonb;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import io.quarkus.arc.DefaultBean;

@Singleton
public class JsonbProducer {

    @Produces
    @DefaultBean
    public JsonbConfig jsonbConfig(Instance<JsonbConfigCustomizer> customizers) {
        JsonbConfig jsonbConfig = new JsonbConfig();
        for (JsonbConfigCustomizer customizer : customizers) {
            customizer.customize(jsonbConfig);
        }
        return jsonbConfig;
    }

    @Produces
    @DefaultBean
    public Jsonb jsonb(JsonbConfig jsonbConfig) {
        return JsonbBuilder.create(jsonbConfig);
    }
}
