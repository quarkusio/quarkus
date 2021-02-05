package io.quarkus.logging.sentry;

import static io.quarkus.logging.sentry.SentryLoggerTest.getSentryHandler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Handler;

import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.sentry.HubAdapter;
import io.sentry.Sentry;
import io.sentry.SentryOptions;

public class SentryLoggerCustomTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .withConfigurationResource("application-sentry-logger-custom.properties");

    @Test
    public void sentryLoggerCustomTest() {
        final Handler sentryHandler = getSentryHandler();
        final SentryOptions options = HubAdapter.getInstance().getOptions();
        assertThat(sentryHandler).isNotNull();
        assertThat(sentryHandler.getLevel()).isEqualTo(org.jboss.logmanager.Level.TRACE);
        assertThat(options.getInAppIncludes()).contains("io.quarkus.logging.sentry").contains("org.test");
        assertThat(options.getDsn()).isEqualTo("https://123@example.com/22222");
        assertThat(sentryHandler).extracting("minimumEventLevel").isEqualTo(Level.INFO);
        assertThat(sentryHandler).extracting("minimumBreadcrumbLevel").isEqualTo(Level.DEBUG);
        assertThat(Sentry.isEnabled()).isTrue();
    }
}
