package io.quarkus.observability.test;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.observability.test.support.DevResourcesTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevResourcesTestProfile.class)
@DisabledOnOs(OS.WINDOWS)
public class DevResourcesMetricsTest extends MetricsTestBase {
}
