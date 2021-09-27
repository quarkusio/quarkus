package io.quarkus.grpc.lock.detection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.blocking.call.test.CallBlocking;
import io.quarkus.grpc.blocking.call.test.CallBlockingGrpc;
import io.quarkus.grpc.blocking.call.test.CallHello;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class BlockingClientCallOnEventLoopTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, CallBlockingService.class, CallBlocking.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addPackage(CallBlockingGrpc.class.getPackage()));

    @GrpcClient
    CallBlocking callBlocking;

    @Test
    void shouldThrowExceptionOnBlockingClientCall() {
        Uni<CallHello.SuccessOrFailureDescription> result = callBlocking.doBlockingCall(CallHello.Empty.getDefaultInstance());
        CallHello.SuccessOrFailureDescription response = result.await().atMost(Duration.ofSeconds(10));

        assertThat(response.getSuccess()).isFalse();

        assertThat(response.getErrorDescription()).contains(CallBlockingService.EXPECTED_ERROR_PREFIX);
    }

}
