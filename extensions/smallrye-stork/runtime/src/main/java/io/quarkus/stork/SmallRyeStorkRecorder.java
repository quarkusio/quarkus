package io.quarkus.stork;

import java.util.List;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.config.ServiceConfig;
import io.vertx.core.Vertx;

@Recorder
public class SmallRyeStorkRecorder {

    public void initialize(ShutdownContext shutdown, RuntimeValue<Vertx> vertx, StorkConfiguration configuration) {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(configuration);
        StorkConfigProvider.init(serviceConfigs);
        Stork.initialize(new QuarkusStorkInfrastructure(vertx.getValue()));
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                Stork.shutdown();
            }
        });
    }
}
