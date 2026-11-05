package io.quarkus.grpc.examples.stork;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(reply.getMessage()).isEqualTo("Hello metrics-blocking");

        assertStorkMetricsInMicrometerRegistry();
        assertStorkObservation();
    }

    @Test
    void shouldExposeStorkMetricsAfterMutinyGrpcCall() {
        HelloReply reply = mutinyStub
                .sayHello(HelloRequest.newBuilder().setName("metrics-mutiny").build())
                .await().atMost(Duration.ofSeconds(5));
        assertThat(reply.getMessage()).isEqualTo("Hello metrics-mutiny");

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

        assertThat(instanceCounter).isNotNull();
        assertThat(serviceDiscoveryDuration).isNotNull();
        assertThat(serviceSelectionDuration).isNotNull();
        assertThat(serviceDiscoveryFailures).isNotNull();
        assertThat(serviceSelectionFailures).isNotNull();
        assertThat(serviceSelectionDuration.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
        assertThat(instanceCounter.count()).isGreaterThan(0);
        assertThat(serviceDiscoveryDuration.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
        assertThat(serviceDiscoveryFailures.count()).isZero();
        assertThat(serviceSelectionFailures.count()).isZero();
    }

    private void assertStorkObservation() {
        StorkObservation metrics = StorkObservationCollectorBean.STORK_METRICS
                .get(SERVICE_NAME + StorkObservationCollectorBean.METRICS_SUFFIX);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getDiscoveredInstancesCount()).isGreaterThan(0);
        assertThat(metrics.getServiceName()).isEqualTo(SERVICE_NAME);
        assertThat(metrics.isDone()).isTrue();
        assertThat(metrics.failure()).isNull();
        assertThat(metrics.getServiceDiscoveryType()).isEqualTo("static");
        assertThat(metrics.getServiceSelectionType()).isEqualTo("round-robin");
        assertThat(metrics.getServiceDiscoveryDuration()).isNotNull();
        assertThat(metrics.getServiceSelectionDuration()).isNotNull();
    }
}
