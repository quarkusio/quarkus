package io.quarkus.grpc.examples.stork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.micrometer.runtime.binder.stork.StorkObservationCollectorBean;
import io.smallrye.stork.api.observability.StorkObservation;

class GrpcStorkMetricsTestBase {

    private static final String SERVICE_NAME = "hello-service";

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingStub;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyStub;

    @Inject
    MeterRegistry registry;

    @Test
    void shouldExposeStorkMetricsAfterBlockingGrpcCall() {
        HelloReply reply = blockingStub.sayHello(HelloRequest.newBuilder().setName("metrics-blocking").build());
        assertEquals("Hello metrics-blocking", reply.getMessage());

        assertStorkMetricsInMicrometerRegistry();
        assertStorkObservation();
    }

    @Test
    void shouldExposeStorkMetricsAfterMutinyGrpcCall() {
        HelloReply reply = mutinyStub
                .sayHello(HelloRequest.newBuilder().setName("metrics-mutiny").build())
                .await().atMost(Duration.ofSeconds(5));
        assertEquals("Hello metrics-mutiny", reply.getMessage());

        assertStorkMetricsInMicrometerRegistry();
        assertStorkObservation();
    }

    private void assertStorkMetricsInMicrometerRegistry() {
        Counter instanceCounter = registry.find("stork.service-discovery.instances.count")
                .tags("service-name", SERVICE_NAME)
                .counter();
        Timer serviceDiscoveryDuration = registry.find("stork.service-discovery.duration")
                .tags("service-name", SERVICE_NAME)
                .timer();
        Timer serviceSelectionDuration = registry.find("stork.service-selection.duration")
                .tags("service-name", SERVICE_NAME)
                .timer();
        Counter serviceDiscoveryFailures = registry.find("stork.service-discovery.failures")
                .tags("service-name", SERVICE_NAME)
                .counter();
        Counter serviceSelectionFailures = registry.find("stork.service-selection.failures")
                .tags("service-name", SERVICE_NAME)
                .counter();

        assertNotNull(instanceCounter);
        assertNotNull(serviceDiscoveryDuration);
        assertNotNull(serviceSelectionDuration);
        assertNotNull(serviceDiscoveryFailures);
        assertNotNull(serviceSelectionFailures);
        assertTrue(serviceSelectionDuration.totalTime(TimeUnit.NANOSECONDS) > 0);
        assertTrue(instanceCounter.count() > 0);
        assertTrue(serviceDiscoveryDuration.totalTime(TimeUnit.NANOSECONDS) > 0);
        assertEquals(0, serviceDiscoveryFailures.count());
        assertEquals(0, serviceSelectionFailures.count());
    }

    private void assertStorkObservation() {
        StorkObservation metrics = StorkObservationCollectorBean.STORK_METRICS
                .get(SERVICE_NAME + StorkObservationCollectorBean.METRICS_SUFFIX);

        assertNotNull(metrics);
        assertTrue(metrics.getDiscoveredInstancesCount() > 0);
        assertEquals(SERVICE_NAME, metrics.getServiceName());
        assertTrue(metrics.isDone());
        assertNull(metrics.failure());
        assertEquals("static", metrics.getServiceDiscoveryType());
        assertEquals("round-robin", metrics.getServiceSelectionType());
        assertNotNull(metrics.getServiceDiscoveryDuration());
        assertNotNull(metrics.getServiceSelectionDuration());
    }
}
