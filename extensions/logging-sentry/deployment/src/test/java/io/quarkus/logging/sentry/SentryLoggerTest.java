package io.quarkus.logging.sentry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
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
        final Handler sentryHandler = getSentryHandler();
        assertThat(sentryHandler).isNotNull();
        assertThat(sentryHandler).extracting("options").extracting("InAppIncludes").satisfies(o -> {
            assertThat(o).isInstanceOfSatisfying(Collection.class, c -> {
                assertThat(c).isEmpty();
            });
        });
        assertThat(sentryHandler).extracting("options").extracting("dsn").isEqualTo("https://123@default.com/22222");
        assertThat(sentryHandler.getLevel()).isEqualTo(org.jboss.logmanager.Level.WARN);
        assertThat(Sentry.isEnabled()).isTrue();
    }

    public static Handler getSentryHandler() {
        LogManager logManager = LogManager.getLogManager();
        assertThat(logManager).isInstanceOf(org.jboss.logmanager.LogManager.class);

        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers()).contains(delayedHandler);
        assertThat(delayedHandler.getLevel()).isEqualTo(Level.ALL);

        Handler handler = Arrays.stream(delayedHandler.getHandlers())
                .filter(h -> SentryHandler.class.getName().equals(h.getClass().getName()))
                .findFirst().orElse(null);
        return handler;
    }

}
