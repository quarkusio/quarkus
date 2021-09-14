package io.quarkus.grpc.client.interceptors;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterBean;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloRequestOrBuilder;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.MutinyHelloService;
import io.quarkus.test.QuarkusUnitTest;

public class ClientInterceptorPriorityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MutinyHelloService.class, MyFirstClientInterceptor.class,
                            MySecondClientInterceptor.class,
                            GreeterGrpc.class, Greeter.class, GreeterBean.class, HelloRequest.class, HelloReply.class,
                            MutinyGreeterGrpc.class,
                            HelloRequestOrBuilder.class, HelloReplyOrBuilder.class))
            .withConfigurationResource("hello-config.properties");

    @Inject
    MyFirstClientInterceptor interceptor1;

    @Inject
    MySecondClientInterceptor interceptor2;

    @GrpcClient("hello-service")
    GreeterGrpc.GreeterBlockingStub client;

    @Test
    public void testInterceptors() {
        HelloReply reply = client
                .sayHello(HelloRequest.newBuilder().setName("neo").build());
        assertThat(reply.getMessage()).isEqualTo("Hello neo");

        assertThat(interceptor1.getLastCall()).isNotZero();
        assertThat(interceptor2.getLastCall()).isNotZero();

        assertThat(interceptor2.getLastCall()).isGreaterThan(interceptor1.getLastCall());
    }
}
