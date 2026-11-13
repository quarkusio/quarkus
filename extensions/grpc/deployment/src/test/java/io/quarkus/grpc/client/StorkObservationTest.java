package io.quarkus.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloRequestOrBuilder;
import io.grpc.examples.helloworld.HelloWorldProto;
import io.quarkus.arc.Unremovable;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.stork.api.observability.ObservationCollector;
import io.smallrye.stork.api.observability.StorkEventHandler;
import io.smallrye.stork.api.observability.StorkObservation;

/**
 * Stork observations (used by the Micrometer Stork metrics) must cover both discovery and selection for gRPC clients.
 */
public class StorkObservationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(HelloWorldProto.class.getPackage())
                    .addClasses(RecordingCollector.class, GreeterGrpc.class, GreeterGrpc.GreeterBlockingStub.class,
                            HelloService.class, HelloRequest.class, HelloReply.class,
                            HelloReplyOrBuilder.class, HelloRequestOrBuilder.class))
            .overrideConfigKey("quarkus.grpc.clients.hello.host", "hello-stork")
            .overrideConfigKey("quarkus.grpc.clients.hello.name-resolver", "stork")
            .overrideConfigKey("quarkus.stork.hello-stork.service-discovery.type", "static")
            .overrideConfigKey("quarkus.stork.hello-stork.service-discovery.address-list", "localhost:8081")
            .overrideConfigKey("quarkus.stork.hello-stork.load-balancer.type", "round-robin");

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub client;

    @Inject
    RecordingCollector collector;

    @Test
    void recordsDiscoveryAndSelectionForEachCall() {
        for (int i = 0; i < 3; i++) {
            assertThat(client.sayHello(HelloRequest.newBuilder().setName("neo").build()).getMessage())
                    .isEqualTo("Hello neo");
        }

        assertThat(collector.completed).hasSize(3).allSatisfy(observation -> {
            assertThat(observation.getServiceName()).isEqualTo("hello-stork");
            assertThat(observation.getServiceDiscoveryType()).isEqualTo("static");
            assertThat(observation.getServiceSelectionType()).isEqualTo("round-robin");
            assertThat(observation.isServiceDiscoverySuccessful()).isTrue();
            assertThat(observation.getDiscoveredInstancesCount()).isEqualTo(1);
            assertThat(observation.failure()).isNull();
        });
    }

    @Singleton
    @Unremovable
    public static class RecordingCollector implements ObservationCollector, StorkEventHandler {

        final List<StorkObservation> completed = new CopyOnWriteArrayList<>();

        @Override
        public StorkObservation create(String serviceName, String serviceDiscoveryType, String serviceSelectionType) {
            return new StorkObservation(serviceName, serviceDiscoveryType, serviceSelectionType, this);
        }

        @Override
        public void complete(StorkObservation observation) {
            completed.add(observation);
        }
    }
}
