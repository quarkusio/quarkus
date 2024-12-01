package io.quarkus.smallrye.faulttolerance.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CircuitBreakerConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(CircuitBreakerConfigBean.class, TestConfigExceptionA.class,
                    TestConfigExceptionB.class))
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.CircuitBreakerConfigBean/skipOn\".circuit-breaker.skip-on",
                    "io.quarkus.smallrye.faulttolerance.test.config.TestConfigExceptionA")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.CircuitBreakerConfigBean/failOn\".circuit-breaker.fail-on",
                    "io.quarkus.smallrye.faulttolerance.test.config.TestConfigExceptionA")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.CircuitBreakerConfigBean/delay\".circuit-breaker.delay",
                    "1000")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.CircuitBreakerConfigBean/delay\".circuit-breaker.delay-unit",
                    "millis")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.CircuitBreakerConfigBean/requestVolumeThreshold\".circuit-breaker.request-volume-threshold",
                    "4")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.CircuitBreakerConfigBean/failureRatio\".circuit-breaker.failure-ratio",
                    "0.8")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.CircuitBreakerConfigBean/successThreshold\".circuit-breaker.success-threshold",
                    "2");

    @Inject
    private CircuitBreakerConfigBean bean;

    @Test
    public void failOn() {
        assertThatThrownBy(() -> bean.failOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failOn()).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.failOn()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void testConfigureSkipOn() {
        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
    }

    @Test
    public void delay() {
        assertThatThrownBy(() -> bean.delay(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.delay(true)).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.delay(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);

        long start = System.nanoTime();
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            assertThatCode(() -> bean.delay(false)).doesNotThrowAnyException();
        });
        long end = System.nanoTime();

        long durationInMillis = Duration.ofNanos(end - start).toMillis();
        assertThat(durationInMillis).isGreaterThan(800);
        assertThat(durationInMillis).isLessThan(2000);
    }

    @Test
    public void requestVolumeThreshold() {
        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.requestVolumeThreshold()).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void failureRatio() {
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatCode(() -> bean.failureRatio(false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatCode(() -> bean.failureRatio(false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThatThrownBy(() -> bean.failureRatio(true)).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.failureRatio(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void successThreshold() {
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> bean.successThreshold(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        }

        assertThatThrownBy(() -> bean.successThreshold(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            assertThatCode(() -> bean.successThreshold(false)).doesNotThrowAnyException();
        });

        assertThatCode(() -> bean.successThreshold(false)).doesNotThrowAnyException();

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> bean.successThreshold(true)).isExactlyInstanceOf(TestConfigExceptionA.class);
        }

        assertThatThrownBy(() -> bean.successThreshold(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            assertThatCode(() -> bean.successThreshold(false)).doesNotThrowAnyException();
        });

        assertThatThrownBy(() -> bean.successThreshold(true)).isExactlyInstanceOf(TestConfigExceptionA.class);

        assertThatThrownBy(() -> bean.successThreshold(false)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }
}
