package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.test.common.TestLog;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.config.SmallRyeConfig;

@QuarkusIntegrationTest
public class TestLogITCase extends TestLogTestCase {

    private static final Logger LOGGER = Logger.getLogger(TestLogITCase.class);

    // Injected automatically by QuarkusIntegrationTestExtension
    private ValueRegistry valueRegistry;

    // Injected automatically by QuarkusIntegrationTestExtension
    private TestLog testLog;

    @Override
    protected Path expectedQuarkusTestLogPath() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        return getLogPath(this.valueRegistry, config);
    }

    @Test
    public void shouldAccessTestLogFilePathFromInjectedValueRegistry() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        Path logPath = getLogPath(this.valueRegistry, config);
        LOGGER.infof("From Injected ValueRegistry - log file path: %s", logPath);
        assertNotNull(logPath);
    }

    @Test
    public void shouldAccessTestLogFilePathFromParameterValueRegistry(ValueRegistry paramValueRegistry) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        Path logPath = getLogPath(paramValueRegistry, config);
        LOGGER.infof("From Parameter ValueRegistry - log file path: %s", logPath);
        assertNotNull(logPath);
    }

    @Test
    public void shouldAccessTestLogFilePathFromInjectedTestLog() {
        Path logPath = testLog.getLogFilePath();
        LOGGER.infof("From Injected TestLog - log file path: %s", logPath);
        assertNotNull(logPath);
    }

    @Test
    public void shouldAccessTestLogFilePathFromParameterTestLog(TestLog testLog) {
        Path logPath = testLog.getLogFilePath();
        LOGGER.infof("From Parameter TestLog - log file path: %s", logPath);
        assertNotNull(logPath);
    }

    protected Path getLogPath(ValueRegistry valueRegistry, SmallRyeConfig config) {
        TestLog testLog = ((valueRegistry != null) && (valueRegistry.containsKey(TestLog.TEST_LOG)))
                ? valueRegistry.get(TestLog.TEST_LOG)
                : null;
        if (testLog != null) {
            return testLog.getLogFilePath();
        }
        LogRuntimeConfig logRuntimeConfig = config.getConfigMapping(LogRuntimeConfig.class);
        return logRuntimeConfig.file().path().toPath();
    }
}
