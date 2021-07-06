package io.quarkus.infinispan.client.deployment.devconsole;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.infinispan.client.runtime.InfinispanClientSupplier;

public class InfinispanDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem remoteCacheManager() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("remoteCacheManager", new InfinispanClientSupplier());
    }
}
