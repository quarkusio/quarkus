package io.quarkus.amazon.lambda.http.deployment;

import io.quarkus.amazon.lambda.deployment.EventServerOverrideBuildItem;
import io.quarkus.amazon.lambda.runtime.MockRestEventServer;
import io.quarkus.deployment.annotations.BuildStep;

public class DevServicesRestLambdaProcessor {

    @BuildStep
    public EventServerOverrideBuildItem overrideEventServer() {
        return new EventServerOverrideBuildItem(
                () -> new MockRestEventServer());
    }
}
