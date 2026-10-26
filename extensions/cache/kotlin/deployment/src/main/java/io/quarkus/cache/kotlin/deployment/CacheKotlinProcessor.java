package io.quarkus.cache.kotlin.deployment;

import io.quarkus.cache.kotlin.runtime.CacheKotlinSuspendMethodHandler;
import io.quarkus.cache.runtime.CacheRecorder;
import io.quarkus.cache.runtime.CacheSpecialMethodHandler;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildStep;

public class CacheKotlinProcessor {

    @BuildStep
    void registerSpecialMethodHandler(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar
                .forService("io.quarkus.cache.kotlin.suspend-method-handler")
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> {
                    CacheSpecialMethodHandler handler = new CacheKotlinSuspendMethodHandler();
                    CacheRecorder.addSpecialMethodHandler(handler);
                    ctx.onStop(() -> CacheRecorder.removeSpecialMethodHandler(handler));
                });
    }
}
