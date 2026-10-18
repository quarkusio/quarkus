package io.quarkus.jdbc.db2.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Docker not supported on Windows")
@DisabledIf(value = "isArm64", disabledReason = "The DB2 image is not published for arm64")
public class LicenseAcceptanceDB2ErrorTestCase {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.license-acceptance", "nonexistent-image:for-error-test")
            .assertException(t -> {
                Throwable current = t;
                boolean found = false;
                while (current != null) {
                    if (current.getMessage() != null
                            && current.getMessage().contains("quarkus.devservices.license-acceptance")) {
                        found = true;
                        break;
                    }
                    current = current.getCause();
                }
                assertThat(found)
                        .as("Expected exception mentioning quarkus.devservices.license-acceptance, but got: %s", t)
                        .isTrue();
            });

    @Test
    public void testErrorMessage() {
    }

    static boolean isArm64() {
        return "aarch64".equals(System.getProperty("os.arch"));
    }
}
