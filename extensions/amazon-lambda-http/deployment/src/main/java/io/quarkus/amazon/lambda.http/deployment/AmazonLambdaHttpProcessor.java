package io.quarkus.amazon.lambda.http.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;

public class AmazonLambdaHttpProcessor {
    @BuildStep
    public RequireVirtualHttpBuildItem requestVirtualHttp(LaunchModeBuildItem launchMode) {
        return launchMode.getLaunchMode() == LaunchMode.NORMAL ? RequireVirtualHttpBuildItem.MARKER : null;
    }
}
