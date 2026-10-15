package io.quarkus.jackson.runtime;

import io.quarkus.jackson.JsonFactoryBuilderCustomizer;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.util.RecyclerPool;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class VertxHybridPoolJsonFactoryBuilderCustomizer implements JsonFactoryBuilderCustomizer {

    @Override
    public void customize(JsonFactoryBuilder builder) {
        try {
            // we need to resort to reflection as we can't directly reference `io.vertx.core.json.jackson.v3.JacksonCodec` due to it being part of the MR resources
            Class<?> poolClass = Class.forName("io.vertx.core.json.jackson.v3.HybridJacksonPool", true,
                    Thread.currentThread().getContextClassLoader());
            Object pool = poolClass.getMethod("getInstance").invoke(null);
            builder.recyclerPool((RecyclerPool) pool);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create instance of `io.vertx.core.json.jackson.v3.HybridJacksonPool`",
                    e);
        }
    }
}
