package io.quarkus.observability.runtime;

import io.quarkus.observability.devresource.DevResources;

public class DevResourceShutdownRecorder {
    public static void shutdown() {
        DevResources.stop();
    }
}
