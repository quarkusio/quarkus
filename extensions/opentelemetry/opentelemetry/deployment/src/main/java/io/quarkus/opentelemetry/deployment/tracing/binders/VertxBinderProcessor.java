package io.quarkus.opentelemetry.deployment.tracing.binders;

import static io.quarkus.bootstrap.classloading.QuarkusClassLoader.isClassPresentAtRuntime;
import static javax.interceptor.Interceptor.Priority.LIBRARY_AFTER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.opentelemetry.deployment.tracing.DropNonApplicationUrisBuildItem;
import io.quarkus.opentelemetry.deployment.tracing.DropStaticResourcesBuildItem;
import io.quarkus.opentelemetry.deployment.tracing.TracerEnabled;
import io.quarkus.opentelemetry.runtime.tracing.binders.vertx.VertxRecorder;
import io.quarkus.vertx.core.deployment.VertxOptionsConsumerBuildItem;
import io.quarkus.vertx.http.deployment.spi.FrameworkEndpointsBuildItem;
import io.quarkus.vertx.http.deployment.spi.StaticResourcesBuildItem;

public class VertxBinderProcessor {
    static class MetricsExtensionAvailable implements BooleanSupplier {
        private static final boolean IS_MICROMETER_EXTENSION_AVAILABLE = isClassPresentAtRuntime(
                "io.quarkus.micrometer.runtime.binder.vertx.VertxHttpServerMetrics");

        @Override
        public boolean getAsBoolean() {
            return IS_MICROMETER_EXTENSION_AVAILABLE && ConfigProvider.getConfig()
                    .getOptionalValue("quarkus.micrometer.binder.http-server.enabled", Boolean.class).orElse(true);
        }
    }

    @BuildStep(onlyIf = TracerEnabled.class)
    void dropNames(
            Optional<FrameworkEndpointsBuildItem> frameworkEndpoints,
            Optional<StaticResourcesBuildItem> staticResources,
            BuildProducer<DropNonApplicationUrisBuildItem> dropNonApplicationUris,
            BuildProducer<DropStaticResourcesBuildItem> dropStaticResources) {

        // Drop framework paths
        List<String> nonApplicationUris = new ArrayList<>();
        frameworkEndpoints.ifPresent(
                frameworkEndpointsBuildItem -> nonApplicationUris.addAll(frameworkEndpointsBuildItem.getEndpoints()));
        dropNonApplicationUris.produce(new DropNonApplicationUrisBuildItem(nonApplicationUris));

        // Drop Static Resources
        List<String> resources = new ArrayList<>();
        if (staticResources.isPresent()) {
            for (StaticResourcesBuildItem.Entry entry : staticResources.get().getEntries()) {
                if (!entry.isDirectory()) {
                    resources.add(entry.getPath());
                }
            }
        }
        dropStaticResources.produce(new DropStaticResourcesBuildItem(resources));
    }

    @BuildStep(onlyIf = TracerEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    VertxOptionsConsumerBuildItem vertxTracingOptions(VertxRecorder recorder) {
        return new VertxOptionsConsumerBuildItem(recorder.getVertxTracingOptions(), LIBRARY_AFTER);
    }

    @BuildStep(onlyIf = TracerEnabled.class, onlyIfNot = MetricsExtensionAvailable.class)
    @Record(ExecutionTime.STATIC_INIT)
    VertxOptionsConsumerBuildItem vertxTracingMetricsOptions(VertxRecorder recorder) {
        return new VertxOptionsConsumerBuildItem(recorder.getVertxTracingMetricsOptions(), LIBRARY_AFTER + 1);
    }

}
