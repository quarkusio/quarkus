package io.quarkus.vertx.http.deployment.devmode;

import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.spi.DeploymentFailedStartHandler;
import io.quarkus.devui.deployment.menu.ConfigurationProcessor;

public class DevModeFailedStartHandler implements DeploymentFailedStartHandler {
    @Override
    public void handleFailedInitialStart() {
        CurrentConfig.EDITOR = ConfigurationProcessor::updateConfig;
    }
}
