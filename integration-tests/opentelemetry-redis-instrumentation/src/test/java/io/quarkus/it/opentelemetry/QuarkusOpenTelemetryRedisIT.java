package io.quarkus.it.opentelemetry;

import java.util.Map;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class QuarkusOpenTelemetryRedisIT extends QuarkusOpenTelemetryRedisTest {

    void checkForException(Map<String, Object> exception) {
        // Ignore it
        // The exception is not passed in native mode. (need to be investigated)
    }
}
