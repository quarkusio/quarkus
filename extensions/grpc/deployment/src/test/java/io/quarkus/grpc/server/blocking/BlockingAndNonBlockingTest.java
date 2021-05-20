package io.quarkus.grpc.server.blocking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.EmptyProtos;

import grpc.health.v1.HealthGrpc;
import grpc.health.v1.HealthOuterClass;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.reflection.v1alpha.MutinyServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServiceResponse;
import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.TestServiceGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.server.services.AssertHelper;
import io.quarkus.grpc.server.services.BlockingMutinyHelloService;
import io.quarkus.grpc.server.services.TestService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class BlockingAndNonBlockingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(HealthGrpc.class.getPackage())
                            .addPackage(GreeterGrpc.class.getPackage())
                            .addPackage(TestServiceGrpc.class.getPackage())
                            .addPackage(EmptyProtos.class.getPackage())
                            .addClasses(BlockingMutinyHelloService.class, TestService.class, AssertHelper.class))
            .withConfigurationResource("blocking-config.properties");

    @Inject
    GrpcHealthStorage healthService;

    @GrpcClient("reflection-service")
    MutinyServerReflectionGrpc.MutinyServerReflectionStub reflection;

    @GrpcClient("test-service")
    TestServiceGrpc.TestServiceBlockingStub test;

    @GrpcClient("greeter-service")
    GreeterGrpc.GreeterBlockingStub greeter;

    @Test
    public void testInvokingABlockingService() {
        HelloReply reply = greeter.sayHello(HelloRequest.newBuilder().setName("neo").build());
        assertThat(reply.getMessage()).contains("executor-thread", "neo");
    }

    @Test
    public void testInvokingANonBlockingService() {
        Messages.SimpleResponse reply = test.unaryCall(Messages.SimpleRequest.newBuilder().build());
        assertThat(reply).isNotNull();
    }

    @Test
    public void testHealth() {
        assertThat(healthService.getStatuses()).contains(
                entry(GreeterGrpc.SERVICE_NAME, HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING),
                entry(TestServiceGrpc.SERVICE_NAME, HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING)

        );
    }

    @Test
    public void testReflection() {
        ServerReflectionRequest request = ServerReflectionRequest.newBuilder().setHost("localhost")
                .setListServices("").build();

        ServerReflectionResponse response = invoke(request);
        List<ServiceResponse> list = response.getListServicesResponse().getServiceList();
        assertThat(list).hasSize(3)
                .anySatisfy(r -> assertThat(r.getName()).isEqualTo(GreeterGrpc.SERVICE_NAME))
                .anySatisfy(r -> assertThat(r.getName()).isEqualTo(TestServiceGrpc.SERVICE_NAME))
                .anySatisfy(r -> assertThat(r.getName()).isEqualTo("grpc.health.v1.Health"));
    }

    private ServerReflectionResponse invoke(ServerReflectionRequest request) {
        return reflection.serverReflectionInfo(Multi.createFrom().item(request))
                .collect().first()
                .await().indefinitely();
    }

}
