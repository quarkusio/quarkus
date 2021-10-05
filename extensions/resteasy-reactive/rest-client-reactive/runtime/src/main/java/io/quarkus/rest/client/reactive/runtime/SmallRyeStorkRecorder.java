package io.quarkus.rest.client.reactive.runtime;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.stork.Stork;

@Recorder
public class SmallRyeStorkRecorder {

    public void initialize(ShutdownContext shutdown) {
        Stork.initialize();
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                Stork.shutdown();
            }
        });
    }
}
