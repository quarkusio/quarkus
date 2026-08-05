package io.quarkus.spiffe.client.deployment.test;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "SPIRE agent uses named pipes on Windows, but currently Vert.x does not support them")
class SpiffeClientUdsTest extends AbstractSpiffeClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest();
}
