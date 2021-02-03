package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

public class RestClientProcessor {
    // Avoid referencing optional dependencies

    // Rest client listener SPI
    private static final String REST_CLIENT_LISTENER_CLASS_NAME = "org.eclipse.microprofile.rest.client.spi.RestClientListener";
    private static final Class<?> REST_CLIENT_LISTENER_CLASS = MicrometerRecorder
            .getClassForName(REST_CLIENT_LISTENER_CLASS_NAME);

    // Rest Client listener
    private static final String REST_CLIENT_METRICS_LISTENER = "io.quarkus.micrometer.runtime.binder.RestClientMetrics";
    // Http Client runtime config (injected programmatically)
    private static final String REST_CLIENT_HTTP_CONFIG = "io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig";

    static class RestClientEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return REST_CLIENT_LISTENER_CLASS != null && mConfig.checkBinderEnabledWithDefault(mConfig.binder.httpClient);
        }
    }

    @BuildStep(onlyIf = { RestClientEnabled.class, MicrometerProcessor.HttpClientBinderEnabled.class })
    UnremovableBeanBuildItem registerRestClientListener(BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/org.eclipse.microprofile.rest.client.spi.RestClientListener"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, REST_CLIENT_METRICS_LISTENER));

        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNameExclusion(REST_CLIENT_HTTP_CONFIG));
    }
}
