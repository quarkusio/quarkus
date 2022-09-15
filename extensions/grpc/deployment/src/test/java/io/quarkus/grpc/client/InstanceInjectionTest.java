package io.quarkus.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Channel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloRequestOrBuilder;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusUnitTest;

public class InstanceInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(GreeterGrpc.class, GreeterGrpc.GreeterBlockingStub.class,
                            MutinyGreeterGrpc.MutinyGreeterStub.class, MutinyGreeterGrpc.class,
                            HelloRequest.class, HelloReply.class,
                            HelloReplyOrBuilder.class, HelloRequestOrBuilder.class))
            .withConfigurationResource("hello-config.properties");

    @GrpcClient("hello-service")
    Instance<Channel> channel;

    @GrpcClient("hello-service-2")
    Instance<GreeterGrpc.GreeterBlockingStub> blocking;

    @GrpcClient("hello-service")
    Instance<MutinyGreeterGrpc.MutinyGreeterStub> mutiny;

    @Test
    public void test() {
        assertThat(channel.isUnsatisfied()).isFalse();
        assertThat(blocking.isUnsatisfied()).isFalse();
        assertThat(mutiny.isUnsatisfied()).isFalse();

        assertThat(channel.get()).isNotNull();
        assertThat(blocking.get()).isNotNull();
        assertThat(mutiny.get()).isNotNull();
    }

}
