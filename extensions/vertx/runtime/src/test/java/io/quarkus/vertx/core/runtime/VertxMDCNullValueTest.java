package io.quarkus.vertx.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * SLF4J allows {@code MDC.put(key, null)}, and libraries such as Apache Pulsar 5 rely on it.
 * A {@code null} value must remove the key instead of failing with a {@link NullPointerException}.
 */
class VertxMDCNullValueTest {

    private static final String KEY = "nullValueTestKey";

    private static Vertx vertx;

    @BeforeAll
    static void startVertx() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void closeVertx() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @AfterEach
    void cleanThreadLocal() {
        VertxMDC.INSTANCE.removeObject(KEY, null);
    }

    @Test
    void putObjectNullRemovesExistingKeyWithoutContext() {
        VertxMDC.INSTANCE.putObject(KEY, "value", null);

        assertThat(VertxMDC.INSTANCE.putObject(KEY, null, null)).isEqualTo("value");
        assertThat(VertxMDC.INSTANCE.copyObject(null)).doesNotContainKey(KEY);
    }

    @Test
    void putNullRemovesExistingKeyWithoutContext() {
        VertxMDC.INSTANCE.put(KEY, "value", null);

        assertThat(VertxMDC.INSTANCE.put(KEY, null, null)).isEqualTo("value");
        assertThat(VertxMDC.INSTANCE.copy(null)).doesNotContainKey(KEY);
    }

    @Test
    void putNullForAbsentKeyWithoutContext() {
        assertThat(VertxMDC.INSTANCE.putObject(KEY, null, null)).isNull();
        assertThat(VertxMDC.INSTANCE.copyObject(null)).doesNotContainKey(KEY);
    }

    @Test
    void putNullRemovesExistingKeyOnVertxContext() throws Exception {
        // Quarkus runs application code on duplicated contexts, which hold the MDC
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
        CompletableFuture<String> previous = new CompletableFuture<>();
        CompletableFuture<Map<String, Object>> remaining = new CompletableFuture<>();
        context.runOnContext(ignored -> {
            try {
                VertxMDC.INSTANCE.put(KEY, "value");
                previous.complete(VertxMDC.INSTANCE.put(KEY, null));
                remaining.complete(VertxMDC.INSTANCE.copyObject());
            } catch (Throwable t) {
                previous.completeExceptionally(t);
                remaining.completeExceptionally(t);
            }
        });

        assertThat(previous.get(10, TimeUnit.SECONDS)).isEqualTo("value");
        assertThat(remaining.get(10, TimeUnit.SECONDS)).doesNotContainKey(KEY);
    }

    @Test
    void putNonNullValueIsStillStored() {
        assertThat(VertxMDC.INSTANCE.putObject(KEY, "value", null)).isNull();
        assertThat(VertxMDC.INSTANCE.getObject(KEY, null)).isEqualTo("value");
    }

    @Test
    void nullKeyIsStillRejected() {
        assertThatNullPointerException().isThrownBy(() -> VertxMDC.INSTANCE.putObject(null, "value", null));
        assertThatNullPointerException().isThrownBy(() -> VertxMDC.INSTANCE.putObject(null, null, null));
    }
}
