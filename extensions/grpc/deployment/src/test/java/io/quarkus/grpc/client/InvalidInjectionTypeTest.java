package io.quarkus.grpc.client;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloRequestOrBuilder;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidInjectionTypeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(GreeterGrpc.class, GreeterGrpc.GreeterStub.class,
                            MutinyGreeterGrpc.MutinyGreeterStub.class, MutinyGreeterGrpc.class,
                            HelloRequest.class, HelloReply.class,
                            HelloReplyOrBuilder.class, HelloRequestOrBuilder.class))
            .setExpectedException(DeploymentException.class);

    @Test
    public void runTest() {
        fail();
    }

    @ApplicationScoped
    static class MyConsumer {

        @GrpcClient
        GreeterGrpc.GreeterStub stub;

    }
}
