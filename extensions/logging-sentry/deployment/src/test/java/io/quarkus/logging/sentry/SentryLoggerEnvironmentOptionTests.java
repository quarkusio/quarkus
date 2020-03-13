package io.quarkus.logging.sentry;

import static io.quarkus.logging.sentry.SentryLoggerTest.getSentryHandler;
import static io.sentry.jvmti.ResetFrameCache.resetFrameCache;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.sentry.Sentry;
import io.sentry.jul.SentryHandler;

public class SentryLoggerEnvironmentOptionTests {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .withConfigurationResource("application-sentry-logger-environment-option.properties");

    @Test
    public void sentryLoggerEnvironmentOptionTest() {
        final SentryHandler sentryHandler = getSentryHandler();
        assertThat(sentryHandler).isNotNull();
        assertThat(Sentry.getStoredClient()).isNotNull();
        assertThat(Sentry.getStoredClient().getEnvironment()).isEqualTo("test-environment");
        assertThat(Sentry.isInitialized()).isTrue();
    }

    @AfterAll
    public static void reset() {
        resetFrameCache();
    }
}
