package io.quarkus.smallrye.faulttolerance.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RetryConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(RetryConfigBean.class, TestException.class,
                    TestConfigExceptionA.class, TestConfigExceptionB.class, TestConfigExceptionB1.class))
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RetryConfigBean/maxRetries\".retry.max-retries",
                    "10")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RetryConfigBean/maxDuration\".retry.max-duration",
                    "1")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RetryConfigBean/maxDuration\".retry.max-duration-unit",
                    "seconds")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RetryConfigBean/delay\".retry.delay",
                    "2000")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RetryConfigBean/delay\".retry.delay-unit",
                    "micros")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RetryConfigBean/retryOn\".retry.retry-on",
                    "io.quarkus.smallrye.faulttolerance.test.config.TestConfigExceptionA,io.quarkus.smallrye.faulttolerance.test.config.TestConfigExceptionB")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RetryConfigBean/abortOn\".retry.abort-on",
                    "io.quarkus.smallrye.faulttolerance.test.config.TestConfigExceptionA,io.quarkus.smallrye.faulttolerance.test.config.TestConfigExceptionB1")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RetryConfigBean/jitter\".retry.jitter",
                    "1")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RetryConfigBean/jitter\".retry.jitter-unit",
                    "seconds");

    @Inject
    private RetryConfigBean bean;

    @Test
    public void maxRetries() {
        AtomicInteger counter = new AtomicInteger();
        assertThatThrownBy(() -> bean.maxRetries(counter)).isExactlyInstanceOf(TestException.class);
        assertThat(counter).hasValue(11);
    }

    @Test
    public void maxDuration() {
        long startTime = System.nanoTime();
        assertThatThrownBy(() -> bean.maxDuration()).isExactlyInstanceOf(TestException.class);
        long endTime = System.nanoTime();

        Duration duration = Duration.ofNanos(endTime - startTime);
        assertThat(duration).isLessThan(Duration.ofSeconds(8));
    }

    @Test
    public void delay() {
        long startTime = System.nanoTime();
        assertThatThrownBy(() -> bean.delay()).isExactlyInstanceOf(TestException.class);
        long endTime = System.nanoTime();

        Duration duration = Duration.ofNanos(endTime - startTime);
        assertThat(duration).isLessThan(Duration.ofSeconds(8));
    }

    @Test
    public void retryOn() {
        AtomicInteger counter = new AtomicInteger();

        counter.set(0);
        assertThatThrownBy(() -> bean.retryOn(new TestException(), counter)).isExactlyInstanceOf(TestException.class);
        assertThat(counter).hasValue(1);

        counter.set(0);
        assertThatThrownBy(() -> bean.retryOn(new TestConfigExceptionA(), counter))
                .isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThat(counter).hasValue(2);

        counter.set(0);
        assertThatThrownBy(() -> bean.retryOn(new TestConfigExceptionB(), counter))
                .isExactlyInstanceOf(TestConfigExceptionB.class);
        assertThat(counter).hasValue(2);

        counter.set(0);
        assertThatThrownBy(() -> bean.retryOn(new TestConfigExceptionB1(), counter))
                .isExactlyInstanceOf(TestConfigExceptionB1.class);
        assertThat(counter).hasValue(2);
    }

    @Test
    public void abortOn() {
        AtomicInteger counter = new AtomicInteger();

        counter.set(0);
        assertThatThrownBy(() -> bean.abortOn(new TestException(), counter)).isExactlyInstanceOf(TestException.class);
        assertThat(counter).hasValue(1);

        counter.set(0);
        assertThatThrownBy(() -> bean.abortOn(new TestConfigExceptionA(), counter))
                .isExactlyInstanceOf(TestConfigExceptionA.class);
        assertThat(counter).hasValue(1);

        counter.set(0);
        assertThatThrownBy(() -> bean.abortOn(new TestConfigExceptionB(), counter))
                .isExactlyInstanceOf(TestConfigExceptionB.class);
        assertThat(counter).hasValue(2);

        counter.set(0);
        assertThatThrownBy(() -> bean.abortOn(new TestConfigExceptionB1(), counter))
                .isExactlyInstanceOf(TestConfigExceptionB1.class);
        assertThat(counter).hasValue(1);
    }

    @Test
    public void jitter() {
        assertThatThrownBy(() -> bean.jitter()).isExactlyInstanceOf(TestConfigExceptionA.class);
    }
}
