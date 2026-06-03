package io.quarkus.amazon.lambda.http.deployment;

import io.quarkus.amazon.lambda.deployment.EventServerOverrideBuildItem;
import io.quarkus.amazon.lambda.deployment.EventServerPortOverrideBuildItem;
import io.quarkus.amazon.lambda.runtime.MockHttpEventServer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;

public class DevServicesHttpLambdaProcessor {

    @BuildStep
    public EventServerOverrideBuildItem overrideEventServer() {
        return new EventServerOverrideBuildItem(
                () -> new MockHttpEventServer());
    }

    @BuildStep
    void adjustEventServerPortForDevMode(LaunchModeBuildItem launchMode,
            BuildProducer<EventServerPortOverrideBuildItem> portOverride) {
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            // In dev mode, the real Vert.x HTTP server starts on the default port (for Dev UI),
            // so the mock event server must use an ephemeral port to avoid conflict
            portOverride.produce(new EventServerPortOverrideBuildItem(0));
        }
    }
}
