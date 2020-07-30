package io.quarkus.it.micrometer.prometheus;

import io.quarkus.test.junit.NativeImageTest;

/**
 * tests that application.properties is read from src/main/resources when running native image tests
 *
 * This does not necessarily belong here, but main and test-extension have a lot of existing
 * config that would need to be duplicated, so it is here out of convenience.
 */
@NativeImageTest
class PrometheusMetricsRegistryIT extends PrometheusMetricsRegistryTest {
}
