package io.quarkus.test;

import java.util.logging.LogRecord;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StrictValidationDisabledTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.log.levle", "INFO") // Intentional typo: "levle" instead of "level"
            .failOnUnknownProperties(false)
            .setLogRecordPredicate(record -> "io.quarkus.config".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                // We want logs if failure on unknown properties is disabled
                Assertions.assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .contains(
                                "Unrecognized configuration key \"%s\" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo");
            });

    @Test
    public void applicationShouldRun() {
        // Application should start
    }
}
