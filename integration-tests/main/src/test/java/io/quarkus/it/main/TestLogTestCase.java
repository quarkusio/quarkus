package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.SmallRyeConfig;

@QuarkusTest
public class TestLogTestCase {

    private static final Logger LOGGER = Logger.getLogger(TestLogTestCase.class);

    protected Path expectedQuarkusTestLogPath() {
        return null;
    }

    @Test
    public void shouldReadQuarkusLogFilePath() {
        Path expectedQuarkusLogPath = Path.of("target" + File.separator + "quarkus.log");

        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        Path quarkusLogFilePath = config.getValue("quarkus.log.file.path", Path.class);
        LOGGER.infof("[quarkus.log.file.path]: %s", quarkusLogFilePath);

        assertEquals(expectedQuarkusLogPath, quarkusLogFilePath);
    }

    @Test
    public void shouldReadQuarkusTestLogFilePath() {
        Path expectedQuarkusTestLogPath = expectedQuarkusTestLogPath();

        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        Path quarkusTestLogFilePath = config.getOptionalValue("quarkus.test.log.file.path", Path.class).orElse(null);
        LOGGER.infof("[quarkus.test.log.file.path]: %s", quarkusTestLogFilePath);

        if (expectedQuarkusTestLogPath == null) {
            assertNull(quarkusTestLogFilePath);
        } else {
            assertEquals(expectedQuarkusTestLogPath, quarkusTestLogFilePath);
        }
    }
}
