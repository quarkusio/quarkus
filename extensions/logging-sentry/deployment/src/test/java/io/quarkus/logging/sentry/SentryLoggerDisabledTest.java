package io.quarkus.logging.sentry;

import static io.quarkus.logging.sentry.SentryLoggerTest.getSentryHandler;
import static io.sentry.jvmti.ResetFrameCache.resetFrameCache;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.sentry.jul.SentryHandler;

public class SentryLoggerDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .withConfigurationResource("application-sentry-logger-disabled.properties");

    @Test
    public void sentryLoggerDisabledTest() {
        final SentryHandler sentryHandler = getSentryHandler();
        assertThat(sentryHandler).isNull();
    }

    @AfterAll
    static void reset() {
        resetFrameCache();
    }
}
