package io.quarkus.test;

import java.util.logging.LogRecord;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StrictValidationEnabledTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.log.levle", "INFO") // Intentional typo: "levle" instead of "level"
            .failOnUnknownProperties(true)
            .assertException(throwable -> {
                // We expect this test to fail due to the unknown property
                if (throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Build failed due to unrecognized configuration properties") &&
                        throwable.getMessage().contains("quarkus.log.levle")) {
                    // This is the expected behavior - the strict validation caught our typo
                    return;
                }
                // If we get here, the test failed for an unexpected reason
                throw new AssertionError("Expected strict property validation failure for typo 'quarkus.log.levle', but got: "
                        + throwable.getMessage());
            })
            .assertLogRecords(records -> {
                // We don't want logs we want failures
                Assertions.assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .doesNotContain(
                                "Unrecognized configuration key \"%s\" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo");
            });

    @Test
    public void shouldNotReachThis() {
        throw new AssertionError(
                "This test method should never execute because QuarkusUnitTest setup should fail first due to the typo");
    }
}