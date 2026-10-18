package io.quarkus.grpc.example.streaming;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.StatusRuntimeException;
import io.grpc.examples.streaming.Streaming;
import io.grpc.examples.streaming.StringReply;
import io.grpc.examples.streaming.StringRequest;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@SuppressWarnings("NewClassNamingConvention")
public class LongStreamTestBase {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @GrpcClient("streaming")
    Streaming streamSvc;

    @Test
    @Timeout(10)
    public void testQuickFailure() {
        Multi<StringRequest> multi = Multi.createFrom().range(1, 1000)
                // delaying stream to make it a bit longer
                .call(() -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.of(1000, ChronoUnit.NANOS)))
                .map(x -> StringRequest.newBuilder()
                        .setAnyValue(x.toString())
                        .build())
                .select().first(10);

        UniAssertSubscriber<StringReply> subscriber = streamSvc.quickStringStream(multi)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber
                .awaitFailure()
                .assertFailedWith(StatusRuntimeException.class);
    }

    @Test
    @Timeout(10)
    public void testMidFailure() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Multi<StringRequest> multi = Multi.createFrom().range(1, 1000)
                // delaying stream to make it a bit longer
                .call(() -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.of(500, ChronoUnit.MILLIS)))
                .map(x -> StringRequest.newBuilder()
                        .setAnyValue(x.toString())
                        .build())
                .onCancellation().invoke(() -> cancelled.set(true))
                .select().first(10);

        UniAssertSubscriber<StringReply> subscriber = streamSvc.midStringStream(multi)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber
                .awaitFailure()
                .assertFailedWith(StatusRuntimeException.class);

        await().untilAtomic(cancelled, is(true));
    }

    @Test
    @Timeout(10)
    public void testQuickFailureWithBidi() {
        Multi<StringRequest> multi = Multi.createFrom().range(1, 1000)
                // delaying stream to make it a bit longer
                .call(() -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.of(500, ChronoUnit.MILLIS)))
                .map(x -> StringRequest.newBuilder()
                        .setAnyValue(x.toString())
                        .build())
                .select().first(10);

        AssertSubscriber<StringReply> subscriber = streamSvc.quickStringBiDiStream(multi)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber
                .awaitFailure()
                .assertFailedWith(StatusRuntimeException.class);
    }

    @Timeout(10)
    @RepeatedTest(5)
    public void testMidFailureWithBiDi() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Multi<StringRequest> multi = Multi.createFrom().range(1, 1000)
                // delaying stream to make it a bit longer
                .call(() -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.of(500, ChronoUnit.MILLIS)))
                .map(x -> StringRequest.newBuilder()
                        .setAnyValue(x.toString())
                        .build())
                .onCancellation().invoke(() -> cancelled.set(true)).log("source")
                .select().first(10);

        AssertSubscriber<StringReply> subscriber = streamSvc.midStringBiDiStream(multi)
                .log("downstream")
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitFailure()
                .assertFailedWith(StatusRuntimeException.class);

        await().untilAtomic(cancelled, is(true));
    }

}
