package io.quarkus.ui;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.ui.runtime.UITemplate;
import io.quarkus.undertow.ServletExtensionBuildItem;

public class UIProcesssor {

    /**
     * Config for UI development mode
     */
    @ConfigRoot
    static class UIConfig {
        /**
         * The port of the local development server
         */
        @ConfigItem(defaultValue = "4200")
        int port;
    }

    private UIConfig config;

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    ServletExtensionBuildItem processUi(LaunchModeBuildItem modeBuildItem, UITemplate template) {
        if (modeBuildItem.getLaunchMode() != LaunchMode.DEVELOPMENT) {
            //we only run in dev mode
            return null;
        }
        return new ServletExtensionBuildItem(template.addUiHandler(config.port));
    }

}
