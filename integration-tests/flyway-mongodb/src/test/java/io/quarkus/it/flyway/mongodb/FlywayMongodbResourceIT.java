package io.quarkus.it.flyway.mongodb;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Our Windows CI does not have Docker installed properly")
public class FlywayMongodbResourceIT extends FlywayMongodbResourceTest {
}
