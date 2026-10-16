package io.quarkus.test.timeouts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class TimeLimitInterceptorTest {

    @Test
    void returnsResultWithinTheLimit() throws Throwable {
        assertThat(TimeLimitInterceptor.callWithTimeLimit(Duration.ofSeconds(5), "fast()", () -> "ok")).isEqualTo("ok");
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void interruptsBlockedInvocation() {
        assertThatThrownBy(() -> TimeLimitInterceptor.callWithTimeLimit(Duration.ofMillis(100), "slow()", () -> {
            Thread.sleep(10_000);
            return null;
        })).isInstanceOf(TimeoutException.class)
                .hasMessage("slow() timed out after 100 ms")
                .hasSuppressedException(new InterruptedException("sleep interrupted"));
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void failuresWithinTheLimitArePropagated() {
        assertThatThrownBy(() -> TimeLimitInterceptor.callWithTimeLimit(Duration.ofSeconds(5), "failing()", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class).hasMessage("boom");
    }

    @Test
    @TimeLimit
    void annotatedTestWithinTheLimit() {
        assertThat(TestTimeouts.get()).isPositive();
    }
}
