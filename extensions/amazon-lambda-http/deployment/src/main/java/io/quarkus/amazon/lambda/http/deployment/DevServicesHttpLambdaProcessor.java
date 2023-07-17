package io.quarkus.amazon.lambda.http.deployment;

import io.quarkus.amazon.lambda.deployment.EventServerOverrideBuildItem;
import io.quarkus.amazon.lambda.runtime.MockHttpEventServer;
import io.quarkus.deployment.annotations.BuildStep;

public class DevServicesHttpLambdaProcessor {

    @BuildStep
    public EventServerOverrideBuildItem overrideEventServer() {
        return new EventServerOverrideBuildItem(
                () -> new MockHttpEventServer());
    }
}
