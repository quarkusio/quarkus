package io.quarkus.jackson.runtime;

import com.fasterxml.jackson.core.util.JsonRecyclerPools;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.vertx.core.json.jackson.HybridJacksonPool;

public class VertxHybridPoolObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        var existingMapperPool = objectMapper.getFactory()._getRecyclerPool();
        // if the recycler pool in use is the default jackson one it means that user hasn't
        // explicitly chosen any, so we can replace it with the vert.x virtual thread friendly one
        if (existingMapperPool.getClass() == JsonRecyclerPools.defaultPool().getClass()) {
            objectMapper.getFactory().setRecyclerPool(HybridJacksonPool.getInstance());
        }
    }
}
