package io.quarkus.grpc.server.exc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterBean;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcRemoteException;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class ExceptionCausePropagationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.test-port", "0")
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(GreeterGrpc.class.getPackage())
                            .addClasses(FailingMutinyHelloService.class, GreeterBean.class, HelloRequest.class));

    @GrpcClient
    Greeter greeter;

    @Test
    void shouldPropagateExceptionCauseChain() {
        Uni<HelloReply> result = greeter.sayHello(HelloRequest.newBuilder().setName("fail-with-cause").build());
        assertThatThrownBy(() -> result.await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(t -> {
                    StatusRuntimeException exception = (StatusRuntimeException) t;
                    assertThat(exception.getCause()).isInstanceOf(GrpcRemoteException.class);
                    assertThat(((GrpcRemoteException) exception.getCause()).getExceptionClassName())
                            .isEqualTo(IllegalStateException.class.getName());
                    assertThat(exception.getCause()).hasMessage("middle cause");
                    assertThat(exception.getCause().getCause()).isInstanceOf(GrpcRemoteException.class);
                    assertThat(((GrpcRemoteException) exception.getCause().getCause()).getExceptionClassName())
                            .isEqualTo(IllegalArgumentException.class.getName());
                    assertThat(exception.getCause().getCause()).hasMessage("root cause");
                });
    }

    @GrpcService
    public static class FailingMutinyHelloService implements Greeter {

        @Override
        public Uni<HelloReply> sayHello(HelloRequest request) {
            if ("fail-with-cause".equals(request.getName())) {
                return Uni.createFrom().failure(new RuntimeException("top level",
                        new IllegalStateException("middle cause", new IllegalArgumentException("root cause"))));
            }
            return Uni.createFrom().item(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        }

        @Override
        public Uni<HelloReply> wEIRD(HelloRequest request) {
            return sayHello(request);
        }
    }
}
