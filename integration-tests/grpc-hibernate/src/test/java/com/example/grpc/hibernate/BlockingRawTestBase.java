package com.example.grpc.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.test.TestOuterClass;
import com.example.test.TestRaw;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.timeouts.TestTimeouts;
import io.quarkus.test.timeouts.TimeLimit;
import io.smallrye.mutiny.Multi;

public class BlockingRawTestBase {

    public static final int NO_OF_ELTS = 100;
    public static final TestOuterClass.Empty EMPTY = TestOuterClass.Empty.getDefaultInstance();
    @GrpcClient
    TestRaw client;

    TestRaw getClient() {
        return client;
    }

    @BeforeEach
    void clear() {
        getClient().clear(EMPTY).onFailure().invoke(e -> {
            throw new RuntimeException("Failed to clear items", e);
        }).await().atMost(TestTimeouts.get());
    }

    @Test
    @TimeLimit(multiplier = 3)
    void shouldAdd() {
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < NO_OF_ELTS; i++) {
            String text = "text " + i;
            expected.add(text);
            final int attempt = i;
            getClient().add(TestOuterClass.Item.newBuilder().setText(text).build())
                    .onFailure().invoke(e -> {
                        throw new RuntimeException("Failed to add on attempt " + attempt, e);
                    })
                    .await().atMost(TestTimeouts.get());
        }

        List<String> actual = new CopyOnWriteArrayList<>();
        Multi<TestOuterClass.Item> all = getClient().getAll(EMPTY);
        all.subscribe().with(item -> actual.add(item.getText()));
        await().atMost(TestTimeouts.get())
                .until(() -> actual.size() == NO_OF_ELTS);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @TimeLimit(multiplier = 3)
    void shouldAddViaBidi() {
        List<String> expected = new ArrayList<>();
        List<String> echoed = new CopyOnWriteArrayList<>();
        List<String> actual = new CopyOnWriteArrayList<>();

        Multi<TestOuterClass.Item> request = Multi.createFrom().emitter(
                m -> {
                    for (int i = 0; i < NO_OF_ELTS; i++) {
                        String text = "text " + i;
                        expected.add(text);
                        m.emit(TestOuterClass.Item.newBuilder().setText(text).build());
                    }
                    m.complete();
                });
        getClient().bidi(request).subscribe().with(item -> echoed.add(item.getText()));

        await().atMost(TestTimeouts.get())
                .until(() -> echoed.size() == NO_OF_ELTS);
        assertThat(echoed).containsExactlyInAnyOrderElementsOf(expected);

        Multi<TestOuterClass.Item> all = getClient().getAll(EMPTY);
        all.subscribe().with(item -> actual.add(item.getText()));
        await().atMost(TestTimeouts.get())
                .until(() -> actual.size() == NO_OF_ELTS);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }
}
