package io.quarkus.logging.sentry;

import static io.quarkus.logging.sentry.SentryLoggerTest.getSentryHandler;
import static io.sentry.jvmti.ResetFrameCache.resetFrameCache;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.sentry.jvmti.FrameCache;

public class SentryLoggerCustomTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-sentry-logger-custom.properties");

    @Test
    public void sentryLoggerCustomTest() {
        assertThat(getSentryHandler().getLevel()).isEqualTo(org.jboss.logmanager.Level.TRACE);
        assertThat(FrameCache.shouldCacheThrowable(new IllegalStateException("Test frame"), 1)).isTrue();
    }

    @AfterAll
    static void reset() {
        resetFrameCache();
    }
}
