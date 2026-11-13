package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LettuceResult}.
 */
class LettuceResultTest {

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
