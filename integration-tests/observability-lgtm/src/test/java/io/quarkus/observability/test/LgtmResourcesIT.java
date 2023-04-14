package io.quarkus.observability.test;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@DisabledOnOs(OS.WINDOWS)
public class LgtmResourcesIT extends LgtmResourcesTest {
}
