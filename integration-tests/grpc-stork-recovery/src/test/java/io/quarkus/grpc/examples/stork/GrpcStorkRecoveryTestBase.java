package io.quarkus.grpc.examples.stork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;

/**
 * Reproducer for two ways a {@code stork://} gRPC client failed to recover when
 * the service was unavailable while the client started up.
 * <p>
 * The {@code delayed} Stork service discovery reports no instances for the first
 * few seconds after its first lookup, so the client's initial resolution
 * always sees an empty instance list. The client must then re-resolve and
 * recover on its own once instances appear.
 * <ul>
 * <li>Classic Java gRPC client ({@link GrpcStorkRecoveryTest}): before the fix,
 * {@code GrpcStorkServiceDiscovery} stayed silent on an empty resolution, so
 * gRPC never scheduled another resolution and the channel was wedged in
 * {@code CONNECTING} forever. The same class of bug also skipped {@code onResult}
 * when a refresh returned an unchanged non-empty instance set (covered by
 * {@code GrpcStorkServiceDiscoveryTest} in the gRPC runtime module).</li>
 * <li>Vert.x gRPC client ({@link VertxGrpcStorkRecoveryTest}): before the fix,
 * {@code StorkGrpcChannel} delivered the failed call's close callback on the
 * wrong executor, so a blocking stub call hung instead of failing fast, and the
 * client never got the chance to retry.</li>
 * </ul>
 */
abstract class GrpcStorkRecoveryTestBase {

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub hello;

    @Test
    void channelRecoversWhenServiceAppearsAfterStartup() {
        HelloRequest request = HelloRequest.newBuilder().setName("late").build();

        // While the delayed discovery reports no instances, calls must fail fast.
        assertThatThrownBy(() -> call(request))
                .isInstanceOf(StatusRuntimeException.class);

        // Without a working re-resolution the client stays broken and this
        // never succeeds within the timeout.
        await().atMost(Duration.ofSeconds(30)).ignoreExceptions().untilAsserted(() -> {
            assertThat(call(request).getMessage()).isEqualTo("Hello late");
        });
    }

    private HelloReply call(HelloRequest request) {
        // A deadline keeps a wedged channel from blocking the test indefinitely.
        return hello.withDeadlineAfter(5, TimeUnit.SECONDS).sayHello(request);
    }
}
