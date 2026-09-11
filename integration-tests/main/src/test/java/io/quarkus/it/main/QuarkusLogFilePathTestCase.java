package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.TestLog;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.SmallRyeConfig;

@QuarkusTest
public class QuarkusLogFilePathTestCase {

    private static final Logger LOGGER = Logger.getLogger(QuarkusLogFilePathTestCase.class);

    // Injected automatically
    protected TestLog testLog;

    protected String expectedQuarkusTestLogPathRegexp() {
        return "target" + Pattern.quote(File.separator) + "quarkus\\.log";
    }

    @Test
    public void shouldReadQuarkusLogFilePath() {
        Path expectedQuarkusLogPath = Path.of("target" + File.separator + "quarkus.log");

        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        Path quarkusLogFilePath = config.getValue("quarkus.log.file.path", Path.class);
        LOGGER.infof("Config [quarkus.log.file.path]: %s", quarkusLogFilePath);

        assertEquals(expectedQuarkusLogPath, quarkusLogFilePath);
    }

    @Test
    public void shouldReadQuarkusTestLogFilePath() {
        String expectedRegexp = expectedQuarkusTestLogPathRegexp();

        Path quarkusTestLogFilePath = testLog.getLogFilePath();
        LOGGER.infof("TestLog [%s]: %s", TestLog.LOG_FILE_PATH.key(), quarkusTestLogFilePath);

        assertNotNull(quarkusTestLogFilePath);
        assertTrue(quarkusTestLogFilePath.toString().matches(expectedRegexp));
    }
}
