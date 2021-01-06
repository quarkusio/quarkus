package io.quarkus.logging.sentry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.handlers.DelayedHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.test.QuarkusUnitTest;
import io.sentry.Sentry;

public class SentryLoggerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .withConfigurationResource("application-sentry-logger-default.properties");

    @Test
    public void sentryLoggerDefaultTest() {
        final SentryHandler sentryHandler = getSentryHandler();
        assertThat(sentryHandler).isNotNull();
        assertThat(sentryHandler.getOptions().getInAppIncludes()).isEmpty();
        assertThat(sentryHandler.getOptions().getDsn()).isEqualTo("https://123@default.com/22222");
        assertThat(sentryHandler.getLevel()).isEqualTo(org.jboss.logmanager.Level.WARN);
        assertThat(Sentry.isEnabled()).isTrue();
    }

    public static SentryHandler getSentryHandler() {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Handler handler = Arrays.stream(delayedHandler.getHandlers())
                .filter(h -> (h instanceof SentryHandler))
                .findFirst().orElse(null);
        return (SentryHandler) handler;
    }

}
