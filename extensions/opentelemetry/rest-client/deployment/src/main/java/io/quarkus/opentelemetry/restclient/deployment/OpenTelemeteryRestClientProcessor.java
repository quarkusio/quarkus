package io.quarkus.opentelemetry.restclient.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class OpenTelemeteryRestClientProcessor {

    private static final String QUARKUS_REST_CLIENT_LISTENER = "io.quarkus.opentelemetry.restclient.QuarkusRestClientListener";

    @BuildStep
    void registerRestClientListener(BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/org.eclipse.microprofile.rest.client.spi.RestClientListener"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, QUARKUS_REST_CLIENT_LISTENER));
    }

}
