package io.quarkus.grpc.server.blocking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import grpc.health.v1.HealthGrpc;
import grpc.health.v1.HealthOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.reflection.v1alpha.MutinyServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServiceResponse;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.server.services.BlockingMutinyHelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class BlockingServiceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(HealthGrpc.class.getPackage())
                            .addPackage(GreeterGrpc.class.getPackage())
                            .addClasses(BlockingMutinyHelloService.class))
            .withConfigurationResource("reflection-config.properties");

    protected ManagedChannel channel;

    @Inject
    GrpcHealthStorage healthService;

    @GrpcClient("reflection-service")
    MutinyServerReflectionGrpc.MutinyServerReflectionStub reflection;

    @BeforeEach
    public void init() {
        channel = ManagedChannelBuilder.forAddress("localhost", 9001)
                .usePlaintext()
                .build();
    }

    @AfterEach
    public void shutdown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    public void testInvokingABlockingService() {
        HelloReply reply = GreeterGrpc.newBlockingStub(channel)
                .sayHello(HelloRequest.newBuilder().setName("neo").build());
        assertThat(reply.getMessage()).contains("executor-thread", "neo");
    }

    @Test
    public void testHealth() {
        assertThat(healthService.getStatuses())
                .contains(entry("helloworld.Greeter", HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING));
    }

    @Test
    public void testReflection() {
        ServerReflectionRequest request = ServerReflectionRequest.newBuilder().setHost("localhost")
                .setListServices("").build();

        ServerReflectionResponse response = invoke(request);
        List<ServiceResponse> list = response.getListServicesResponse().getServiceList();
        assertThat(list).hasSize(2)
                .anySatisfy(r -> assertThat(r.getName()).isEqualTo("helloworld.Greeter"))
                .anySatisfy(r -> assertThat(r.getName()).isEqualTo("grpc.health.v1.Health"));
    }

    private ServerReflectionResponse invoke(ServerReflectionRequest request) {
        return reflection.serverReflectionInfo(Multi.createFrom().item(request))
                .collect().first()
                .await().indefinitely();
    }
}
