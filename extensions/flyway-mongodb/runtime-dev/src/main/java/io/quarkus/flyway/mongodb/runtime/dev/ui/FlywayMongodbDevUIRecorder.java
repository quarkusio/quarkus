package io.quarkus.flyway.mongodb.runtime.dev.ui;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FlywayMongodbDevUIRecorder {

    public void initializeJsonRpcService() {
        // Eagerly resolve the service so it's available when Dev UI calls it.
        Arc.container().instance(FlywayMongodbJsonRpcService.class).get();
    }
}
