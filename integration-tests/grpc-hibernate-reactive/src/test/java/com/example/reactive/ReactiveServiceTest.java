package com.example.reactive;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.subscription.Cancellable;

@QuarkusTest
public class ReactiveServiceTest {

    public static final int TIMEOUT = 60;

    @GrpcClient
    ReactiveTest client;

    @Test
    @Timeout(TIMEOUT)
    void shouldWatchAndAddMultipleTimes() {
        List<String> collected = new CopyOnWriteArrayList<>();

        Cancellable watch = client.watch(com.example.reactive.Test.Empty.getDefaultInstance())
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().with(item -> collected.add(item.getText()));
        List<String> expected = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 10; i++) {
            String text = "hello world" + i;
            expected.add(text);
            client.add(com.example.reactive.Test.Item.newBuilder().setText(text).build())
                    .onFailure().invoke(Throwable::printStackTrace)
                    .await().atMost(Duration.ofSeconds(TIMEOUT / 6));
        }

        await().atMost(Duration.ofSeconds(TIMEOUT / 2))
                .until(() -> collected.containsAll(expected));

        watch.cancel();
    }
}
