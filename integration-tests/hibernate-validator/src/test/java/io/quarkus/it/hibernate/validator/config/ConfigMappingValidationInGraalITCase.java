package io.quarkus.it.hibernate.validator.config;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Runs {@link ConfigMappingValidationTest} against the native executable.
 */
@QuarkusIntegrationTest
public class ConfigMappingValidationInGraalITCase extends ConfigMappingValidationTest {
}
