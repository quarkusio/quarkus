package io.quarkus.grpc.example.streaming;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.StatusRuntimeException;
import io.grpc.examples.streaming.Streaming;
import io.grpc.examples.streaming.StringReply;
import io.grpc.examples.streaming.StringRequest;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@SuppressWarnings("NewClassNamingConvention")
public class LongStreamTestBase {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @GrpcClient("streaming")
    Streaming streamSvc;

    @Test
    public void testQuickFailure() {
        Multi<StringRequest> multi = Multi.createFrom().range(1, 10)
                // delaying stream to make it a bit longer
                .call(() -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.of(1000, ChronoUnit.NANOS)))
                .map(x -> StringRequest.newBuilder()
                        .setAnyValue(x.toString())
                        .build());
        //                .invoke(x -> log.info("Stream piece number is: " + x.getAnyValue()));

        UniAssertSubscriber<StringReply> subscriber = streamSvc.quickStringStream(multi)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber
                .awaitFailure()
                .assertFailedWith(StatusRuntimeException.class);
    }

    @Test
    public void testMidFailure() {
        Multi<StringRequest> multi = Multi.createFrom().range(1, 10)
                // delaying stream to make it a bit longer
                .call(() -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.of(1000, ChronoUnit.NANOS)))
                .map(x -> StringRequest.newBuilder()
                        .setAnyValue(x.toString())
                        .build());
        //                .invoke(x -> log.info("Stream piece number is: " + x.getAnyValue()));

        UniAssertSubscriber<StringReply> subscriber = streamSvc.midStringStream(multi)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber
                .awaitFailure()
                .assertFailedWith(StatusRuntimeException.class);
    }

}
