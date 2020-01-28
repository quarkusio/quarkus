package io.quarkus.logging.sentry;

import static io.sentry.jvmti.ResetFrameCache.resetFrameCache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class SentryLoggerWrongTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .withConfigurationResource("application-sentry-logger-wrong.properties")
            .setExpectedException(ConfigurationException.class);

    @Test
    public void sentryLoggerWrongTest() {
        //Exception is expected
    }

    @AfterAll
    static void reset() {
        resetFrameCache();
    }
}
