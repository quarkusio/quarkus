package io.quarkus.resteasy.reactive.server.deployment.devconsole;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.EndpointScoresSupplier;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectScores() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("endpointScores", new EndpointScoresSupplier());
    }

}
