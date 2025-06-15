package io.quarkus.grpc.server.interceptors;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test reproducing <a href="https://github.com/quarkusio/quarkus/issues/26830">#26830</a>.
 */
public class GrpcContextPropagationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(MyFirstInterceptor.class, MyInterceptedGreeting.class));

    @GrpcClient
    Greeter greeter;

    @Test
    void test() {
        HelloReply foo = greeter.sayHello(HelloRequest.newBuilder().setName("foo").build()).await().indefinitely();
        assertThat(foo.getMessage()).isEqualTo("hello k1 - 1");
        foo = greeter.sayHello(HelloRequest.newBuilder().setName("foo").build()).await().indefinitely();
        assertThat(foo.getMessage()).isEqualTo("hello k1 - 2");
    }

}
