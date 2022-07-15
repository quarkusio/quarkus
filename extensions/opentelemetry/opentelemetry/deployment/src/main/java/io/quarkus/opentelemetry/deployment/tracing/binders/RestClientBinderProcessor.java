package io.quarkus.opentelemetry.deployment.tracing.binders;

import static io.quarkus.bootstrap.classloading.QuarkusClassLoader.*;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;
import io.quarkus.opentelemetry.runtime.tracing.binders.restclient.OpenTelemetryClientFilter;

public class RestClientBinderProcessor {

    static class RestClientAvailable implements BooleanSupplier {
        private static final boolean IS_REST_CLIENT_AVAILABLE = isClassPresentAtRuntime(
                "javax.ws.rs.client.ClientRequestFilter");

        @Override
        public boolean getAsBoolean() {
            return IS_REST_CLIENT_AVAILABLE;
        }
    }

    @BuildStep(onlyIf = { OpenTelemetryEnabled.class, RestClientAvailable.class })
    void registerProvider(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexed,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalIndexed.produce(new AdditionalIndexedClassesBuildItem(OpenTelemetryClientFilter.class.getName()));
        additionalBeans.produce(new AdditionalBeanBuildItem(OpenTelemetryClientFilter.class));
    }
}
