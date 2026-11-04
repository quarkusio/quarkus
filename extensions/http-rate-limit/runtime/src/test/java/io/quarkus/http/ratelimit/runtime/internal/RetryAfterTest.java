package io.quarkus.http.ratelimit.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RetryAfterTest {

    @Test
    void ceilsToWholeSeconds() {
        assertThat(RetryAfter.seconds(Duration.ofMillis(1_001))).isEqualTo(2);
        assertThat(RetryAfter.seconds(Duration.ofSeconds(12))).isEqualTo(12);
    }

    @Test
    void neverBelowOne() {
        assertThat(RetryAfter.seconds(Duration.ZERO)).isEqualTo(1);
        assertThat(RetryAfter.seconds(Duration.ofMillis(5))).isEqualTo(1);
    }

    @Test
    void largeValuesAreNotCapped() {
        assertThat(RetryAfter.seconds(Duration.ofHours(1))).isEqualTo(3600);
    }
}
