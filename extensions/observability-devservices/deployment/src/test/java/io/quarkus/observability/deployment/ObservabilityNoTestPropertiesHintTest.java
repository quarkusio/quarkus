package io.quarkus.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that no hint is logged when the application does not require any property provided by
 * observability Dev Services.
 */
public class ObservabilityNoTestPropertiesHintTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setLogRecordPredicate(record -> record.getMessage().contains("quarkus.observability.enabled-in-tests"))
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Test
    void noHintIsLogged() {
    }
}
