package io.quarkus.it.vertx.nativetransport;

import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@EnabledOnOs(OS.LINUX)
@QuarkusIntegrationTest
class ExplicitIoUringTransportIT extends ExplicitIoUringTransportTest {

}
