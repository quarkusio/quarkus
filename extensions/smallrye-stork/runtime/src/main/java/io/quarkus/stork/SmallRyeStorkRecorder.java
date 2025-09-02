package io.quarkus.stork;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.observability.ObservationCollector;
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
        Instance<ObservationCollector> instance = CDI.current().select(ObservationCollector.class);
        if (instance.isResolvable()) {
            Stork.initialize(new QuarkusStorkObservableInfrastructure(vertx.getValue(), instance.get()));
        } else {
            QuarkusStorkInfrastructure infrastructure = new QuarkusStorkInfrastructure(vertx.getValue());
            Stork.initialize(infrastructure);
        }

        shutdown.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                Stork.shutdown();
            }
        });
    }
}
