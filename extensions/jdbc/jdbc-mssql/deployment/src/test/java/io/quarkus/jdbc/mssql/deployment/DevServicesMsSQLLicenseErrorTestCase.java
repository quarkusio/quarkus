package io.quarkus.jdbc.mssql.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DevServicesMsSQLLicenseErrorTestCase {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withConfigurationResource("application-no-license.properties")
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
}
