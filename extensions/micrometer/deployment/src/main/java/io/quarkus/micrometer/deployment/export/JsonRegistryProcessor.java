package io.quarkus.micrometer.deployment.export;

import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.export.JsonMeterRegistryProvider;
import io.quarkus.micrometer.runtime.export.JsonRecorder;
import io.quarkus.micrometer.runtime.registry.json.JsonMeterRegistry;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

public class JsonRegistryProcessor {

    private static final Logger log = Logger.getLogger(JsonRegistryProcessor.class);

    public static class JsonRegistryEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return mConfig.checkRegistryEnabledWithDefault(mConfig.export.json);
        }
    }

    @BuildStep(onlyIf = JsonRegistryEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void initializeJsonRegistry(MicrometerConfig config,
            BuildProducer<MicrometerRegistryProviderBuildItem> registryProviders,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            JsonRecorder recorder) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JsonMeterRegistryProvider.class)
                .setUnremovable().build());
        registryProviders.produce(new MicrometerRegistryProviderBuildItem(JsonMeterRegistry.class));
        routes.produce(new RouteBuildItem.Builder()
                .routeFunction(recorder.route(config.export.json.path))
                .handler(recorder.getHandler())
                .nonApplicationRoute(true)
                .build());
        log.debug("Initialized a JSON meter registry on path=" + config.export.json.path);
    }

}
