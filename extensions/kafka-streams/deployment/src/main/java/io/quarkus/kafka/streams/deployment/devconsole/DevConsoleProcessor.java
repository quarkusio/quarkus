package io.quarkus.kafka.streams.deployment.devconsole;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.kafka.streams.runtime.TopologySupplier;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectInfos() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("topology", new TopologySupplier());
    }

}
