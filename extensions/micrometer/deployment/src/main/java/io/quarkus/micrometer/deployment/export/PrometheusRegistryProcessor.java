package io.quarkus.micrometer.deployment.export;

import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.config.PrometheusConfigGroup;
import io.quarkus.micrometer.runtime.export.PrometheusRecorder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

/**
 * Add support for the Prometheus Meter Registry. Note that the registry may not
 * be available at deployment time for some projects: Avoid direct class
 * references.
 */
public class PrometheusRegistryProcessor {
    private static final Logger log = Logger.getLogger(PrometheusRegistryProcessor.class);

    static final String REGISTRY_CLASS_NAME = "io.micrometer.prometheus.PrometheusMeterRegistry";
    static final Class<?> REGISTRY_CLASS = MicrometerRecorder.getClassForName(REGISTRY_CLASS_NAME);

    public static class PrometheusEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return REGISTRY_CLASS != null && mConfig.checkRegistryEnabledWithDefault(mConfig.export.prometheus);
        }
    }

    @BuildStep(onlyIf = PrometheusEnabled.class)
    MicrometerRegistryProviderBuildItem createPrometheusRegistry(MicrometerConfig config,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Add the Prometheus Registry beans
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                .addBeanClass("io.quarkus.micrometer.runtime.export.PrometheusMeterRegistryProvider")
                .setUnremovable();
        if (config.export.prometheus.defaultRegistry) {
            builder.addBeanClass("io.quarkus.micrometer.runtime.export.PrometheusMeterRegistryProducer");
        }
        additionalBeans.produce(builder.build());

        // Include the PrometheusMeterRegistry in a possible CompositeMeterRegistry
        return new MicrometerRegistryProviderBuildItem(REGISTRY_CLASS);
    }

    @BuildStep(onlyIf = PrometheusEnabled.class)
    @Record(value = ExecutionTime.STATIC_INIT)
    void createPrometheusRoute(BuildProducer<RouteBuildItem> routes,
            MicrometerConfig mConfig,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            PrometheusRecorder recorder) {

        PrometheusConfigGroup pConfig = mConfig.export.prometheus;
        log.debug("PROMETHEUS CONFIG: " + pConfig);

        // Exact match for resources matched to the root path
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .routeFunction(pConfig.path, recorder.route())
                .handler(recorder.getHandler())
                .displayOnNotFoundPage("Metrics")
                .blockingRoute()
                .build());

        // Match paths that begin with the deployment path
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .routeFunction(pConfig.path + (pConfig.path.endsWith("/") ? "*" : "/*"), recorder.route())
                .handler(recorder.getHandler())
                .blockingRoute()
                .build());

        // Fallback paths (for non text/plain requests)
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .routeFunction(pConfig.path, recorder.fallbackRoute())
                .handler(recorder.getFallbackHandler())
                .build());
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .routeFunction(pConfig.path + (pConfig.path.endsWith("/") ? "*" : "/*"), recorder.fallbackRoute())
                .handler(recorder.getFallbackHandler())
                .build());
    }
}
