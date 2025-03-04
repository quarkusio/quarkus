package io.quarkus.micrometer.deployment.export;

import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.micrometer.deployment.MicrometerRegistryProviderBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.config.PrometheusConfigGroup;
import io.quarkus.micrometer.runtime.export.PrometheusRecorder;
import io.quarkus.micrometer.runtime.export.exemplars.EmptyExemplarSamplerProvider;
import io.quarkus.micrometer.runtime.export.exemplars.NoopOpenTelemetryExemplarContextUnwrapper;
import io.quarkus.micrometer.runtime.export.exemplars.OpenTelemetryExemplarContextUnwrapper;
import io.quarkus.micrometer.runtime.export.exemplars.OpentelemetryExemplarSamplerProvider;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * Add support for the Prometheus Meter Registry. Note that the registry may not
 * be available at deployment time for some projects: Avoid direct class
 * references.
 */
@BuildSteps(onlyIf = PrometheusRegistryProcessor.PrometheusEnabled.class)
public class PrometheusRegistryProcessor {
    private static final Logger log = Logger.getLogger(PrometheusRegistryProcessor.class);

    static final String REGISTRY_CLASS_NAME = "io.micrometer.prometheus.PrometheusMeterRegistry";
    static final Class<?> REGISTRY_CLASS = MicrometerRecorder.getClassForName(REGISTRY_CLASS_NAME);

    public static class PrometheusEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return (REGISTRY_CLASS != null) && QuarkusClassLoader.isClassPresentAtRuntime(REGISTRY_CLASS_NAME)
                    && mConfig.checkRegistryEnabledWithDefault(mConfig.export.prometheus);
        }
    }

    public static class TraceEnabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime("io.quarkus.opentelemetry.runtime.OpenTelemetryUtil");
        }
    }

    @BuildStep
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

    @BuildStep(onlyIf = { TraceEnabled.class })
    void registerOpentelemetryExemplarSamplerProvider(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(OpentelemetryExemplarSamplerProvider.class)
                .addBeanClass(OpenTelemetryExemplarContextUnwrapper.class)
                .setUnremovable()
                .build());
    }

    @BuildStep(onlyIfNot = { TraceEnabled.class })
    void registerEmptyExamplarProvider(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(EmptyExemplarSamplerProvider.class)
                .addBeanClass(NoopOpenTelemetryExemplarContextUnwrapper.class)
                .setUnremovable()
                .build());
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    void createPrometheusRoute(BuildProducer<RouteBuildItem> routes,
            BuildProducer<RegistryBuildItem> registries,
            MicrometerConfig mConfig,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem,
            PrometheusRecorder recorder) {

        PrometheusConfigGroup pConfig = mConfig.export.prometheus;
        log.debug("PROMETHEUS CONFIG: " + pConfig);

        // Exact match for resources matched to the root path
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .routeFunction(pConfig.path, recorder.route())
                .routeConfigKey("quarkus.micrometer.export.prometheus.path")
                .handler(recorder.getHandler())
                .displayOnNotFoundPage("Metrics")
                .blockingRoute()
                .build());

        // Match paths that begin with the deployment path
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .routeFunction(pConfig.path + (pConfig.path.endsWith("/") ? "*" : "/*"), recorder.route())
                .handler(recorder.getHandler())
                .blockingRoute()
                .build());

        // Fallback paths (for non text/plain requests)
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .routeFunction(pConfig.path, recorder.fallbackRoute())
                .handler(recorder.getFallbackHandler())
                .build());
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .routeFunction(pConfig.path + (pConfig.path.endsWith("/") ? "*" : "/*"), recorder.fallbackRoute())
                .handler(recorder.getFallbackHandler())
                .build());

        var path = nonApplicationRootPathBuildItem.resolveManagementPath(pConfig.path,
                managementInterfaceBuildTimeConfig, launchModeBuildItem);
        registries.produce(new RegistryBuildItem("Prometheus", path));
    }
}
