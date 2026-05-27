package io.quarkus.jlink.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JLinkSteps#format(Duration)}.
 */
class JLinkStepsFormatTest {

    @Test
    void formatZeroDuration() {
        assertThat(JLinkSteps.format(Duration.ZERO).toString()).isEqualTo("0s");
    }

    @Test
    void formatMillisecondsOnly() {
        assertThat(JLinkSteps.format(Duration.ofMillis(500)).toString()).isEqualTo("0.500s");
    }

    @Test
    void formatSecondsOnly() {
        assertThat(JLinkSteps.format(Duration.ofSeconds(5)).toString()).isEqualTo("5s");
    }

    @Test
    void formatSecondsWithMillis() {
        assertThat(JLinkSteps.format(Duration.ofMillis(5500)).toString()).isEqualTo("5.500s");
    }

    @Test
    void formatMinutesOnly() {
        assertThat(JLinkSteps.format(Duration.ofMinutes(3)).toString()).isEqualTo("3m");
    }

    @Test
    void formatMinutesAndSeconds() {
        assertThat(JLinkSteps.format(Duration.ofSeconds(185)).toString()).isEqualTo("3m5s");
    }

    @Test
    void formatHoursMinutesSeconds() {
        // 1h 2m 3.456s = 3600000 + 120000 + 3456 = 3723456 ms
        assertThat(JLinkSteps.format(Duration.ofMillis(3723456)).toString()).isEqualTo("1h2m3.456s");
    }

    @Test
    void formatSingleDigitMillisZeroPads() {
        // 1.005s
        assertThat(JLinkSteps.format(Duration.ofMillis(1005)).toString()).isEqualTo("1.005s");
    }

    @Test
    void formatTwoDigitMillisZeroPads() {
        // 1.050s
        assertThat(JLinkSteps.format(Duration.ofMillis(1050)).toString()).isEqualTo("1.050s");
    }

    @Test
    void formatHoursOnly() {
        assertThat(JLinkSteps.format(Duration.ofHours(2)).toString()).isEqualTo("2h");
    }
}
