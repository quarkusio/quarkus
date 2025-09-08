package io.quarkus.grpc.examples.cancellation.helper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.google.protobuf.Empty;

import examples.StatusResponse;
import examples.TestService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.Cancellable;

public class CancellationTestRunner {
    private final TestService testService;
    private final AtomicReference<Cancellable> cancellableSubscriber = new AtomicReference<>();

    private AssertSubscriber<Object> statusSubscriber;

    public CancellationTestRunner(TestService testService) {
        this.testService = testService;
    }

    public void start(Supplier<Cancellable> requestToTest) {
        statusSubscriber = testService.readStatus(Empty.getDefaultInstance())
                .map(StatusResponse::getStatus)
                .onItem().invoke(s -> {
                    switch (s) {
                        case READY:
                            // Run request as soon as the status reader is ready.
                            cancellableSubscriber.set(requestToTest.get());
                            break;
                        case SUBSCRIBED:
                            // Do not cancel until the request has reached the server. This is to ensure that the server call must be cancelled.
                            cancelRequest();
                            break;
                        default:
                            break;
                    }
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10));
    }

    public void awaitTermination() {
        try {
            statusSubscriber
                    .awaitCompletion(Duration.ofSeconds(5))
                    .assertItems(StatusResponse.Status.READY, StatusResponse.Status.SUBSCRIBED,
                            StatusResponse.Status.CANCELLED);
        } catch (AssertionError e) {
            Log.error("cancellation failed");
            cancelRequest();
            throw e;
        }
    }

    private void cancelRequest() {
        Cancellable cancellable = cancellableSubscriber.get();
        if (cancellable != null) {
            cancellable.cancel();
        }
    }
}
