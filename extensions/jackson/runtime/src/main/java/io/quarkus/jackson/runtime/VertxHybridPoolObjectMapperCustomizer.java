package io.quarkus.jackson.runtime;

import com.fasterxml.jackson.core.util.JsonRecyclerPools;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.vertx.core.json.jackson.HybridJacksonPool;

public class VertxHybridPoolObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        if (objectMapper.getFactory()._getRecyclerPool() == JsonRecyclerPools.defaultPool()) {
            objectMapper.getFactory().setRecyclerPool(HybridJacksonPool.getInstance());
        }
    }
}
