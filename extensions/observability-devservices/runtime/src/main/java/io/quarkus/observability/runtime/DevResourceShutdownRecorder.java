package io.quarkus.observability.runtime;

import io.quarkus.observability.devresource.DevResources;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DevResourceShutdownRecorder {
    public void shutdown(ShutdownContext context) {
        context.addLastShutdownTask(DevResources::stop);
    }
}
