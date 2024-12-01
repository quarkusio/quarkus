package io.quarkus.smallrye.faulttolerance.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TimeoutConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(TimeoutConfigBean.class))
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.TimeoutConfigBean/value\".timeout.value",
                    "1000")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.TimeoutConfigBean/unit\".timeout.unit",
                    "millis")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.TimeoutConfigBean/both\".timeout.value",
                    "1000")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.TimeoutConfigBean/both\".timeout.unit",
                    "millis");

    @Inject
    private TimeoutConfigBean bean;

    @Test
    public void value() {
        doTest(() -> bean.value());
    }

    @Test
    public void unit() {
        doTest(() -> bean.unit());
    }

    @Test
    public void both() {
        doTest(() -> {
            try {
                bean.both().toCompletableFuture().get(1, TimeUnit.MINUTES);
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
    }

    private void doTest(ThrowingCallable action) {
        long start = System.nanoTime();
        assertThatThrownBy(action).isExactlyInstanceOf(TimeoutException.class);
        long end = System.nanoTime();

        long durationInMillis = Duration.ofNanos(end - start).toMillis();
        assertThat(durationInMillis).isGreaterThan(800);
        assertThat(durationInMillis).isLessThan(2000);
    }
}
