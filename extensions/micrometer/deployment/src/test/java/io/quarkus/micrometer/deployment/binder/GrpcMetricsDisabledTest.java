package io.quarkus.micrometer.deployment.binder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GrpcMetricsDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.grpc-client.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.grpc-server.enabled", "true")

            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .withEmptyApplication();

    @Inject
    BeanManager beans;

    @Test
    void testNoInstancePresentIfNoGrpcClasses() {
        assertTrue(
                beans.createInstance().select()
                        .stream().filter(this::isClientMetricInterceptor).findAny().isEmpty(),
                "No io.quarkus.micrometer.runtime.binder.grpc.ClientGrpcMetrics expected, because we don't have dependency to grpc");

        assertTrue(
                beans.createInstance().select()
                        .stream().filter(this::isServerMetricInterceptor).findAny().isEmpty(),
                "No io.quarkus.micrometer.runtime.binder.grpc.ServerGrpcMetrics expected, because we don't have dependency to grpc");
    }

    private boolean isClientMetricInterceptor(Object bean) {
        return bean.getClass().toString().equals("io.quarkus.micrometer.runtime.binder.grpc.ClientGrpcMetrics");
    }

    private boolean isServerMetricInterceptor(Object bean) {
        return bean.getClass().toString().equals("io.quarkus.micrometer.runtime.binder.grpc.ServerGrpcMetrics");
    }

}
