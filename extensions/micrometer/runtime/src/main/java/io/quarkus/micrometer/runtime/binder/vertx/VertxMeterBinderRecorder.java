package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.VertxOptions;

@Recorder
public class VertxMeterBinderRecorder {
    static final Logger VERTX_LOGGER = Logger.getLogger(VertxMeterBinderRecorder.class);

    static VertxMeterBinderAdapter binderAdapter = new VertxMeterBinderAdapter();
    static volatile HttpBinderConfiguration devModeConfig;

    /* STATIC_INIT */
    public Consumer<VertxOptions> setVertxMetricsOptions() {
        return new Consumer<VertxOptions>() {
            @Override
            public void accept(VertxOptions vertxOptions) {
                vertxOptions.setMetricsOptions(binderAdapter);
            }
        };
    }

    /* RUNTIME_INIT */
    public void configureBinderAdapter() {
        HttpBinderConfiguration httpConfig = Arc.container().instance(HttpBinderConfiguration.class).get();
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            if (devModeConfig == null) {
                // Create an object whose attributes we can update
                devModeConfig = httpConfig.unwrap();
                binderAdapter.setHttpConfig(devModeConfig);
            } else {
                // update config attributes
                devModeConfig.update(httpConfig);
            }
        } else {
            // unwrap the CDI bean (use POJO)
            binderAdapter.setHttpConfig(httpConfig.unwrap());
        }
    }
}
