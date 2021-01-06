package io.quarkus.logging.sentry;

import static io.quarkus.logging.sentry.SentryLoggerTest.getSentryHandler;
import static org.assertj.core.api.Assertions.assertThat;

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
        final SentryHandler sentryHandler = getSentryHandler();
        assertThat(sentryHandler).isNotNull();
        assertThat(sentryHandler.getLevel()).isEqualTo(org.jboss.logmanager.Level.TRACE);
        assertThat(sentryHandler.getOptions().getInAppIncludes()).containsExactlyInAnyOrder("io.quarkus.logging.sentry",
                "org.test");
        assertThat(sentryHandler.getOptions().getDsn()).isEqualTo("https://123@example.com/22222");
        assertThat(Sentry.isEnabled()).isTrue();
    }
}
