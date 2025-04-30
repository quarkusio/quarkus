package io.quarkus.websockets.next.test.telemetry.endpoints.onerror;

import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.websockets.next.OnError;

@Unremovable
@ApplicationScoped
public class GlobalErrorHandler {

    public static final CountDownLatch ILLEGAL_ARGUMENT_EXCEPTION_LATCH = new CountDownLatch(1);

    @OnError
    public String onError(IllegalArgumentException e) {
        ILLEGAL_ARGUMENT_EXCEPTION_LATCH.countDown();
        return e.getMessage();
    }

}
