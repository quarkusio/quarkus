package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

/**
 * Unit tests for {@link LettuceResult}.
 */
class LettuceResultTest {

    @Test
    void toUniShouldConvertCompletedStage() {
        Uni<String> uni = LettuceResult.toUni(() -> CompletableFuture.completedFuture("hello"));
        String result = uni.await().atMost(Duration.ofSeconds(1));
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void toUniShouldConvertNullResult() {
        Uni<String> uni = LettuceResult.toUni(() -> CompletableFuture.completedFuture(null));
        String result = uni.await().atMost(Duration.ofSeconds(1));
        assertThat(result).isNull();
    }

    @Test
    void toUniShouldPropagateException() {
        Uni<String> uni = LettuceResult.toUni(() -> {
            CompletableFuture<String> f = new CompletableFuture<>();
            f.completeExceptionally(new RuntimeException("boom"));
            return f;
        });
        assertThatThrownBy(() -> uni.await().atMost(Duration.ofSeconds(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
    }

    @Test
    void toUniShouldBeLazy() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        Uni<String> uni = LettuceResult.toUni(() -> {
            invoked.set(true);
            return CompletableFuture.completedFuture("lazy");
        });
        // Not yet invoked — lazy
        assertThat(invoked.get()).isFalse();
        // Now subscribe
        String result = uni.await().atMost(Duration.ofSeconds(1));
        assertThat(invoked.get()).isTrue();
        assertThat(result).isEqualTo("lazy");
    }

    @Test
    void toBlockingShouldReturnResult() {
        String result = LettuceResult.toBlocking(
                CompletableFuture.completedFuture("blocked"), Duration.ofSeconds(1));
        assertThat(result).isEqualTo("blocked");
    }

    @Test
    void toBlockingShouldPropagateException() {
        CompletableFuture<String> f = new CompletableFuture<>();
        f.completeExceptionally(new RuntimeException("fail"));
        assertThatThrownBy(() -> LettuceResult.toBlocking(f, Duration.ofSeconds(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("fail");
    }
}
