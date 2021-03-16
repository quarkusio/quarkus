package io.quarkus.jsonb;

import javax.enterprise.context.Dependent;
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
    @Dependent //JsonbConfig is not thread safe so it must not be made singleton.
    @DefaultBean
    public JsonbConfig jsonbConfig(Instance<JsonbConfigCustomizer> customizers) {
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
