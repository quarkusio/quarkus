package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.VertxOptions;

@Recorder
public class VertxMeterBinderRecorder {
    private static final Logger log = Logger.getLogger(VertxMeterBinderRecorder.class);

    /* STATIC_INIT */
    public Consumer<VertxOptions> configureMetricsAdapter() {
        return new Consumer<VertxOptions>() {
            @Override
            public void accept(VertxOptions vertxOptions) {
                log.debug("Adding Micrometer MeterBinder to VertxOptions");
                VertxMeterBinderAdapter binder = Arc.container().instance(VertxMeterBinderAdapter.class).get();
                vertxOptions.setMetricsOptions(binder);
            }
        };
    }

    /* RUNTIME_INIT */
    public void setVertxConfig(VertxConfig config) {
        VertxMeterBinderAdapter binder = Arc.container().instance(VertxMeterBinderAdapter.class).get();
        HttpBinderConfiguration httpConfig = Arc.container().instance(HttpBinderConfiguration.class).get();
        binder.setVertxConfig(config, httpConfig);
    }
}
