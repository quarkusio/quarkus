package io.quarkus.grpc.protov2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.v2.GreeterGrpc;
import io.grpc.examples.helloworld.v2.HelloReply;
import io.grpc.examples.helloworld.v2.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.v2.HelloRequest;
import io.grpc.examples.helloworld.v2.HelloRequestOrBuilder;
import io.grpc.examples.helloworld.v2.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusUnitTest;

public class ProtoV2Test {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloServiceV2.class, MutinyGreeterGrpc.MutinyGreeterStub.class,
                            HelloReply.class, HelloRequest.class, MutinyGreeterGrpc.class, GreeterGrpc.class,
                            HelloRequestOrBuilder.class, HelloReplyOrBuilder.class))
            .withConfigurationResource("hello-config.properties");

    @GrpcClient("hello-service")
    MutinyGreeterGrpc.MutinyGreeterStub stub;

    @Test
    public void testProtoV2() {
        String s = stub.sayHello(HelloRequest.newBuilder().setName("proto v2").build())
                .map(HelloReply::getMessage)
                .await().atMost(Duration.ofSeconds(5));
        assertThat(s).isEqualTo("hello proto v2");
    }

}
