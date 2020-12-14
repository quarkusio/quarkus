package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

public class HttpClientProcessor {
    // Avoid referencing optional dependencies

    // Rest client listener SPI
    private static final String REST_CLIENT_LISTENER_CLASS_NAME = "org.eclipse.microprofile.rest.client.spi.RestClientListener";
    private static final Class<?> REST_CLIENT_LISTENER_CLASS = MicrometerRecorder
            .getClassForName(REST_CLIENT_LISTENER_CLASS_NAME);

    // Rest Client listener
    private static final String REST_CLIENT_METRICS_LISTENER = "io.quarkus.micrometer.runtime.binder.RestClientMetrics";

    static class HttpClientEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return REST_CLIENT_LISTENER_CLASS != null && mConfig.checkBinderEnabledWithDefault(mConfig.binder.httpClient);
        }
    }

    @BuildStep(onlyIf = HttpClientEnabled.class)
    void registerRestClientListener(BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/org.eclipse.microprofile.rest.client.spi.RestClientListener"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, REST_CLIENT_METRICS_LISTENER));
    }
}
