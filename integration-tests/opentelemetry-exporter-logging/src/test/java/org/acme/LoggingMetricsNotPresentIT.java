package org.acme;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@Disabled("Manual only")
@QuarkusIntegrationTest
class LoggingMetricsNotPresentIT extends LoggingMetricsNotPresentTest {
    // Execute the same tests but in packaged mode.
}
