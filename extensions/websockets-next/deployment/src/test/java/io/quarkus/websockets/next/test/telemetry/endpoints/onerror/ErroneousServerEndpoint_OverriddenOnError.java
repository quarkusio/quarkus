package io.quarkus.websockets.next.test.telemetry.endpoints.onerror;

import java.util.concurrent.CountDownLatch;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/server-error-overridden-on-error")
public class ErroneousServerEndpoint_OverriddenOnError {

    public static CountDownLatch RUNTIME_EXCEPTION_LATCH = new CountDownLatch(1);

    @OnBinaryMessage
    public Uni<Dto> onMessage(Multi<Dto> dto) {
        return dto.toUni();
    }

    @OnError
    public void onError(RuntimeException e) {
        RUNTIME_EXCEPTION_LATCH.countDown();
    }
}
