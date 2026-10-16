package io.quarkus.test.timeouts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class TimeLimitEnforcerTest {

    @Test
    void finishingWithinTheLimitDoesNothing() throws Exception {
        TimeLimitEnforcer enforcer = TimeLimitEnforcer.start(Duration.ofSeconds(5), "fast()");
        enforcer.finish();
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void interruptsBlockedThreadAndFails() {
        TimeLimitEnforcer enforcer = TimeLimitEnforcer.start(Duration.ofMillis(100), "slow()");
        assertThatThrownBy(() -> {
            try {
                Thread.sleep(10_000);
            } finally {
                enforcer.finish();
            }
        }).isInstanceOf(TimeoutException.class).hasMessage("slow() timed out after 100 ms");
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    @TimeLimit
    void annotatedTestWithinTheLimit() {
        assertThat(TestTimeouts.get()).isPositive();
    }
}
