package com.example.grpc.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import com.example.test.Test;
import com.example.test.TestOuterClass;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;

@QuarkusTest
public class BlockingMutinyTest {

    public static final int NO_OF_ELTS = 100;
    public static final int TIMEOUT = 60;
    public static final TestOuterClass.Empty EMPTY = TestOuterClass.Empty.getDefaultInstance();
    @GrpcClient
    Test client;

    @BeforeEach
    void clear() {
        client.clear(EMPTY).onFailure().invoke(e -> {
            throw new RuntimeException("Failed to clear items", e);
        }).await().atMost(Duration.ofSeconds(20));
    }

    @org.junit.jupiter.api.Test
    @Timeout(TIMEOUT)
    void shouldAddItems() {
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < NO_OF_ELTS; i++) {
            String text = "text " + i;
            expected.add(text);
            final int attempt = i;
            client.add(TestOuterClass.Item.newBuilder().setText(text).build())
                    .onFailure().invoke(e -> {
                        throw new RuntimeException("Failed to add on attempt " + attempt, e);
                    })
                    .await().atMost(Duration.ofSeconds(5));
        }

        List<String> actual = new ArrayList<>();
        Multi<TestOuterClass.Item> all = client.getAll(EMPTY)
                .onFailure().invoke(th -> {
                    System.out.println("Failed to read");
                    th.printStackTrace();
                });
        all.subscribe().with(item -> actual.add(item.getText()));
        await().atMost(Duration.ofSeconds(TIMEOUT / 2))
                .until(() -> actual.size() == NO_OF_ELTS);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @org.junit.jupiter.api.Test
    @Timeout(TIMEOUT)
    void shouldAddViaBidi() {
        List<String> expected = new ArrayList<>();
        List<String> echoed = new ArrayList<>();
        List<String> actual = new ArrayList<>();

        Multi<TestOuterClass.Item> request = Multi.createFrom().emitter(
                m -> {
                    for (int i = 0; i < NO_OF_ELTS; i++) {
                        String text = "text " + i;
                        expected.add(text);
                        m.emit(TestOuterClass.Item.newBuilder().setText(text).build());
                    }
                    m.complete();
                });
        client.bidi(request).subscribe().with(item -> echoed.add(item.getText()));

        await().atMost(Duration.ofSeconds(TIMEOUT / 2))
                .until(() -> echoed.size() == NO_OF_ELTS);
        assertThat(echoed).containsExactlyInAnyOrderElementsOf(expected);

        Multi<TestOuterClass.Item> all = client.getAll(EMPTY)
                .onFailure().invoke(th -> {
                    System.out.println("Failed to read");
                    th.printStackTrace();
                });
        all.subscribe().with(item -> actual.add(item.getText()));
        await().atMost(Duration.ofSeconds(TIMEOUT / 2))
                .until(() -> actual.size() == NO_OF_ELTS);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }
}
