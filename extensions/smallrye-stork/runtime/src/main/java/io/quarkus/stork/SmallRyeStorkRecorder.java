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
    private final RuntimeValue<StorkConfiguration> runtimeConfig;

    public SmallRyeStorkRecorder(final RuntimeValue<StorkConfiguration> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void initialize(ShutdownContext shutdown, RuntimeValue<Vertx> vertx) {
        List<ServiceConfig> serviceConfigs = StorkConfigUtil.toStorkServiceConfig(runtimeConfig.getValue());
        StorkConfigProvider.init(serviceConfigs);
        Stork.initialize(new QuarkusStorkObservableInfrastructure(vertx.getValue()));

        shutdown.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                Stork.shutdown();
            }
        });
    }

}
