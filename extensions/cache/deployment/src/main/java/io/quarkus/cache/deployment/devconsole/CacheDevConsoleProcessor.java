package io.quarkus.cache.deployment.devconsole;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import io.quarkus.cache.runtime.CaffeineCacheSupplier;
import io.quarkus.cache.runtime.devconsole.CacheDevConsoleRecorder;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;

public class CacheDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectBeanInfo() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("cacheInfos", new CaffeineCacheSupplier());
    }

    @BuildStep
    @Record(value = STATIC_INIT, optional = true)
    DevConsoleRouteBuildItem invokeEndpoint(CacheDevConsoleRecorder recorder) {
        return new DevConsoleRouteBuildItem("caches", "POST", recorder.clearCacheHandler());
    }
}
