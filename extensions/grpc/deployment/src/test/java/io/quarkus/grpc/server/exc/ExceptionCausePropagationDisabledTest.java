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
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class ExceptionCausePropagationDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideRuntimeConfigKey("quarkus.grpc.server.propagate-exception-causes", "false")
            .overrideRuntimeConfigKey("quarkus.grpc.propagate-exception-causes", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(ExceptionCausePropagationTest.FailingMutinyHelloService.class, GreeterBean.class,
                            HelloRequest.class));

    @GrpcClient
    Greeter greeter;

    @Test
    void shouldNotPropagateCauseWhenDisabled() {
        Uni<HelloReply> result = greeter.sayHello(HelloRequest.newBuilder().setName("fail-with-cause").build());
        assertThatThrownBy(() -> result.await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(t -> assertThat(t.getCause()).isNull());
    }
}
