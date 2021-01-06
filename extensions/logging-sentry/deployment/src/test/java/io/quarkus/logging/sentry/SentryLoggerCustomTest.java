package io.quarkus.logging.sentry;

import static io.quarkus.logging.sentry.SentryLoggerTest.getSentryHandler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.sentry.Sentry;

public class SentryLoggerCustomTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .withConfigurationResource("application-sentry-logger-custom.properties");

    @Test
    public void sentryLoggerCustomTest() {
        final Handler sentryHandler = getSentryHandler();
        assertThat(sentryHandler).isNotNull();
        assertThat(sentryHandler.getLevel()).isEqualTo(org.jboss.logmanager.Level.TRACE);
        assertThat(sentryHandler).extracting("options").extracting("InAppIncludes").satisfies(o -> {
            assertThat(o.toString()).contains("io.quarkus.logging.sentry").contains("org.test");
        });
        assertThat(sentryHandler).extracting("options").extracting("dsn").isEqualTo("https://123@example.com/22222");
        assertThat(Sentry.isEnabled()).isTrue();
    }
}
