package io.quarkus.infinispan.client.deployment.devconsole;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.infinispan.client.runtime.InfinispanServerUrlSupplier;

public class InfinispanDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem infinispanServer() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("serverUrl", new InfinispanServerUrlSupplier());
    }
}
