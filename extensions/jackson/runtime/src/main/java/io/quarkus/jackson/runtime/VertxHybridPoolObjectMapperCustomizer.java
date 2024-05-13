package io.quarkus.jackson.runtime;

import com.fasterxml.jackson.core.util.JsonRecyclerPools;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.vertx.core.json.jackson.HybridJacksonPool;

public class VertxHybridPoolObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        var existingMapperPool = objectMapper.getFactory()._getRecyclerPool();
        // JsonRecyclerPools.defaultPool() by default should create a LockFreePool
        if (existingMapperPool instanceof JsonRecyclerPools.LockFreePool) {
            objectMapper.getFactory().setRecyclerPool(HybridJacksonPool.getInstance());
        }
    }
}
