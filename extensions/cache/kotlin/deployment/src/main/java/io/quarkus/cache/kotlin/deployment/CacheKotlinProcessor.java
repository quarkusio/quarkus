package io.quarkus.cache.kotlin.deployment;

import io.quarkus.cache.kotlin.runtime.CacheKotlinRecorder;
import io.quarkus.cache.runtime.CacheRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;

public class CacheKotlinProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerSpecialMethodHandler(CacheRecorder cacheRecorder, CacheKotlinRecorder kotlinRecorder) {
        cacheRecorder.registerSpecialMethodHandler(kotlinRecorder.createSpecialMethodHandler());
    }
}
