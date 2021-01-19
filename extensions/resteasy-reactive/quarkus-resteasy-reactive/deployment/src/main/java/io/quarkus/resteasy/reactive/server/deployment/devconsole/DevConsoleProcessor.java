package io.quarkus.resteasy.reactive.server.deployment.devconsole;

import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.EndpointScoresSupplier;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectScores() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("endpointScores", new EndpointScoresSupplier());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectAdditionalEndpoints(
            List<NotFoundPageDisplayableEndpointBuildItem> additionalEndpoint) {

        return new DevConsoleTemplateInfoBuildItem("additionalEndpointInfo", additionalEndpoint);
    }

}
