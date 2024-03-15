package io.quarkus.it.opentelemetry;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class QuarkusOpenTelemetryRedisIT extends QuarkusOpenTelemetryRedisTest {

    @Override
    String getKey(String k) {
        return "native-" + k;
    }
}
