package io.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Regression test for <a href="https://github.com/quarkusio/quarkus/pull/28702">#28702</a>:
 * checking Docker availability must not leave testcontainers configuration behind,
 * either in-memory (in {@link TestcontainersConfiguration#getUserProperties()})
 * or on disk ({@code ~/.testcontainers.properties}).
 */
class IsContainerRuntimeWorkingTest {

    private static final File TESTCONTAINERS_PROPS_FILE = new File(System.getProperty("user.home"),
            ".testcontainers.properties");

    @Test
    void dockerAvailabilityCheckDoesNotLeaveConfigBehind() throws Exception {
        TestcontainersConfiguration config = TestcontainersConfiguration.getInstance();
        Properties userProperties = config.getUserProperties();

        String originalPropertyValue = userProperties.getProperty("testcontainers.reuse.enable");
        boolean fileExistedBefore = TESTCONTAINERS_PROPS_FILE.exists();
        byte[] originalFileContent = fileExistedBefore ? Files.readAllBytes(TESTCONTAINERS_PROPS_FILE.toPath()) : null;

        try {
            new IsContainerRuntimeWorking.TestContainersStrategy(true).get();

            assertThat(userProperties.getProperty("testcontainers.reuse.enable"))
                    .as("testcontainers.reuse.enable in-memory property should be restored after Docker check")
                    .isEqualTo(originalPropertyValue);

            if (fileExistedBefore) {
                assertThat(Files.readAllBytes(TESTCONTAINERS_PROPS_FILE.toPath()))
                        .as("~/.testcontainers.properties content should be unchanged after Docker check")
                        .isEqualTo(originalFileContent);
            } else {
                assertThat(TESTCONTAINERS_PROPS_FILE)
                        .as("~/.testcontainers.properties should not be created by Docker check")
                        .doesNotExist();
            }
        } finally {
            if (originalPropertyValue != null) {
                userProperties.setProperty("testcontainers.reuse.enable", originalPropertyValue);
            } else {
                userProperties.remove("testcontainers.reuse.enable");
            }
            if (!fileExistedBefore && TESTCONTAINERS_PROPS_FILE.exists()) {
                TESTCONTAINERS_PROPS_FILE.delete();
            } else if (fileExistedBefore) {
                Files.write(TESTCONTAINERS_PROPS_FILE.toPath(), originalFileContent);
            }
        }
    }
}
