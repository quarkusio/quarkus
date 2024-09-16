package io.quarkus.vertx.http.deployment.devmode;

import io.quarkus.dev.spi.DeploymentFailedStartHandler;
import io.quarkus.devui.deployment.menu.ConfigurationProcessor;

public class DevModeFailedStartHandler implements DeploymentFailedStartHandler {
    @Override
    public void handleFailedInitialStart() {
        ConfigurationProcessor.setDefaultConfigEditor();
    }
}
