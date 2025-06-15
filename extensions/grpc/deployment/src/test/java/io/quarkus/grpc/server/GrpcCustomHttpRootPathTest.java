package io.quarkus.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloRequestOrBuilder;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test to check whether grpc service is still accessible when custom quarkus.http.root-path is used other than '/'
 * Refer: https://github.com/quarkusio/quarkus/issues/34261
 */
public class GrpcCustomHttpRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreeterGrpc.class,
                    GreeterGrpc.GreeterBlockingStub.class, HelloService.class, HelloRequest.class, HelloReply.class,
                    HelloReplyOrBuilder.class, HelloRequestOrBuilder.class))
            .withConfigurationResource("grpc-server-custom-http-rootpath-config.properties");

    @GrpcClient("hello-service")
    GreeterGrpc.GreeterBlockingStub service;

    @Test
    public void grpcAndCustomHttpRootPathTest() {

        String response = service.sayHello(HelloRequest.newBuilder().setName("World!").build()).getMessage();
        assertThat(response).isEqualTo("Hello World!");
    }
}
