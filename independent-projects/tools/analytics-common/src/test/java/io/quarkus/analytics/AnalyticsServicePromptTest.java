package io.quarkus.analytics;

import static io.quarkus.analytics.ConfigService.ACCEPTANCE_PROMPT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.analytics.config.FileLocations;
import io.quarkus.analytics.config.TestFileLocationsImpl;
import io.quarkus.analytics.dto.config.LocalConfig;
import io.quarkus.analytics.util.FileUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;

class AnalyticsServicePromptTest extends AnalyticsServiceTestBase {

    private FileLocations fileLocations;
    private AnalyticsService service;

    @BeforeEach
    void setUp() throws IOException {
        fileLocations = new TestFileLocationsImpl(true);
        service = new AnalyticsService(fileLocations, MessageWriter.info());
    }

    @AfterEach
    void tearDown() throws IOException {
        ((TestFileLocationsImpl) fileLocations).deleteAll();
        service = null;
    }

    @Test
    void testConsoleQuestion_yes() throws IOException {
        assertFalse(fileLocations.getLocalConfigFile().toFile().exists());
        service.buildAnalyticsUserInput((String prompt) -> {
            assertEquals(ACCEPTANCE_PROMPT, prompt);
            return "y";
        });
        assertTrue(fileLocations.getLocalConfigFile().toFile().exists());
        Optional<LocalConfig> localConfig = FileUtils.read(LocalConfig.class, fileLocations.getLocalConfigFile(),
                MessageWriter.info());
        assertTrue(localConfig.isPresent());
        assertFalse(localConfig.get().isDisabled());
    }

    @Test
    void testConsoleQuestion_no() throws IOException {
        assertFalse(fileLocations.getLocalConfigFile().toFile().exists());
        service.buildAnalyticsUserInput((String prompt) -> {
            assertEquals(ACCEPTANCE_PROMPT, prompt);
            return "n";
        });
        assertTrue(fileLocations.getLocalConfigFile().toFile().exists());
        Optional<LocalConfig> localConfig = FileUtils.read(LocalConfig.class, fileLocations.getLocalConfigFile(),
                MessageWriter.info());
        assertTrue(localConfig.isPresent());
        assertTrue(localConfig.get().isDisabled());
    }

    @Test
    void testConsoleQuestion_promptTimeout() throws IOException {
        try {
            System.setProperty("quarkus.analytics.prompt.timeout", "0");
            assertFalse(fileLocations.getLocalConfigFile().toFile().exists());
            service.buildAnalyticsUserInput((String prompt) -> {
                assertEquals(ACCEPTANCE_PROMPT, prompt);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return "n";
            });
            assertFalse(fileLocations.getLocalConfigFile().toFile().exists());
        } finally {
            System.clearProperty("quarkus.analytics.prompt.timeout");
        }
    }

    @Test
    void testConsoleQuestion_AnalyticsDisabled() throws IOException {
        try {
            System.setProperty("quarkus.analytics.disabled", "true");
            assertFalse(fileLocations.getLocalConfigFile().toFile().exists());
            service.buildAnalyticsUserInput((String prompt) -> {
                fail("Prompt should be disabled");
                return "n";
            });
            assertFalse(fileLocations.getLocalConfigFile().toFile().exists());
        } finally {
            System.clearProperty("quarkus.analytics.disabled");
        }
    }
}
