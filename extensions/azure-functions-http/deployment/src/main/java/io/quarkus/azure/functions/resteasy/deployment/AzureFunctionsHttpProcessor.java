package io.quarkus.azure.functions.resteasy.deployment;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarRequiredBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;

public class AzureFunctionsHttpProcessor {
    private static final Logger log = Logger.getLogger(AzureFunctionsHttpProcessor.class);

    @BuildStep
    public UberJarRequiredBuildItem forceUberJar() {
        // Azure Functions needs a single JAR inside a dedicated directory
        return new UberJarRequiredBuildItem();
    }

    @BuildStep
    public RequireVirtualHttpBuildItem requestVirtualHttp(LaunchModeBuildItem launchMode) {
        return launchMode.getLaunchMode() == LaunchMode.NORMAL ? RequireVirtualHttpBuildItem.MARKER : null;
    }
}
