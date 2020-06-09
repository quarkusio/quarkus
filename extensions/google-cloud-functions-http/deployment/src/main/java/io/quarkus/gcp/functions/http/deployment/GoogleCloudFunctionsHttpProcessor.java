package io.quarkus.gcp.functions.http.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;

public class GoogleCloudFunctionsHttpProcessor {
    private static final String FEATURE_NAME = "google-cloud-functions-http";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    public RunTimeConfigurationDefaultBuildItem disableBanner() {
        // the banner is not displayed well inside the Google Cloud Function logs
        return new RunTimeConfigurationDefaultBuildItem("quarkus.banner.enabled", "false");
    }

    @BuildStep
    public RequireVirtualHttpBuildItem requestVirtualHttp(LaunchModeBuildItem launchMode) {
        return launchMode.getLaunchMode() == LaunchMode.NORMAL ? RequireVirtualHttpBuildItem.MARKER : null;
    }
}
