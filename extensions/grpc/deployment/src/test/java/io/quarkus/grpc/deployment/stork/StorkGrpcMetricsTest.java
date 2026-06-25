package io.quarkus.grpc.deployment.stork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.micrometer.runtime.binder.stork.StorkObservationCollectorBean;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.stork.api.observability.StorkObservation;

public class StorkGrpcMetricsTest {

    private static final String SERVICE_NAME = "hello-service";
    private static final int GRPC_PORT = 9901;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setFlatClassPath(true)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloStorkGrpcService.class)
                    .addPackage(MutinyGreeterGrpc.class.getPackage()))
            .overrideConfigKey("quarkus.grpc.clients.hello.host", "hello-service")
            .overrideConfigKey("quarkus.grpc.clients.hello.name-resolver", "stork")
            .overrideConfigKey("quarkus.grpc.server.test-port", String.valueOf(GRPC_PORT))
            .overrideConfigKey("quarkus.grpc.clients.hello.port", String.valueOf(GRPC_PORT))
            .overrideConfigKey("quarkus.stork.hello-service.service-discovery.type", "static")
            .overrideConfigKey("quarkus.stork.hello-service.service-discovery.address-list", "localhost:" + GRPC_PORT)
            .overrideConfigKey("quarkus.stork.hello-service.load-balancer.type", "round-robin")
            .overrideConfigKey("quarkus.grpc.clients.hello.use-quarkus-grpc-client", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false");

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
        assertEquals(0, serviceDiscoveryFailures.count());
        assertEquals(0, serviceSelectionFailures.count());
        if (instanceCounter.count() > 0) {
            assertTrue(serviceDiscoveryDuration.totalTime(TimeUnit.NANOSECONDS) > 0);
        }
    }

    private void assertStorkObservation() {
        StorkObservation metrics = StorkObservationCollectorBean.STORK_METRICS
                .get(SERVICE_NAME + StorkObservationCollectorBean.METRICS_SUFFIX);

        assertNotNull(metrics);
        assertEquals(SERVICE_NAME, metrics.getServiceName());
        assertTrue(metrics.isDone());
        assertNull(metrics.failure());
        assertEquals("static", metrics.getServiceDiscoveryType());
        assertEquals("round-robin", metrics.getServiceSelectionType());
        assertNotNull(metrics.getServiceSelectionDuration());
        if (metrics.getDiscoveredInstancesCount() > 0) {
            assertNotNull(metrics.getServiceDiscoveryDuration());
        }
    }
}
