package io.quarkus.stork;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.stork.Stork;
import io.vertx.core.Vertx;

@Recorder
public class SmallRyeStorkRecorder {

    public void initialize(ShutdownContext shutdown, RuntimeValue<Vertx> vertx) {
        Stork.initialize(new QuarkusStorkInfrastructure(vertx.getValue()));
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                Stork.shutdown();
            }
        });
    }
}
