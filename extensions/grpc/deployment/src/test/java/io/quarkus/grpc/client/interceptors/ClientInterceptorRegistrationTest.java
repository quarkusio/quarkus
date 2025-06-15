package io.quarkus.grpc.client.interceptors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

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
import io.quarkus.grpc.RegisterClientInterceptor;
import io.quarkus.grpc.server.services.MutinyHelloService;
import io.quarkus.test.QuarkusUnitTest;

public class ClientInterceptorRegistrationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(MutinyHelloService.class,
                    MyThirdClientInterceptor.class, MyLastClientInterceptor.class, Calls.class, GreeterGrpc.class,
                    Greeter.class, GreeterBean.class, HelloRequest.class, HelloReply.class, MutinyGreeterGrpc.class,
                    HelloRequestOrBuilder.class, HelloReplyOrBuilder.class))
            .withConfigurationResource("hello-config.properties");

    @RegisterClientInterceptor(MyLastClientInterceptor.class)
    @RegisterClientInterceptor(MyThirdClientInterceptor.class)
    @GrpcClient("hello-service")
    GreeterGrpc.GreeterBlockingStub client;

    @Test
    public void testInterceptorRegistration() {
        Calls.LIST.clear();

        HelloReply reply = client.sayHello(HelloRequest.newBuilder().setName("neo").build());
        assertThat(reply.getMessage()).isEqualTo("Hello neo");

        List<String> calls = Calls.LIST;
        assertEquals(2, calls.size());
        assertEquals(MyThirdClientInterceptor.class.getName(), calls.get(0));
        assertEquals(MyLastClientInterceptor.class.getName(), calls.get(1));
    }
}
