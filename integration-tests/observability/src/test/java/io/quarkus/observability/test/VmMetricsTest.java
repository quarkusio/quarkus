package io.quarkus.observability.test;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.observability.test.support.VmTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(VmTestProfile.class)
@DisabledOnOs(OS.WINDOWS)
public class VmMetricsTest extends MetricsTestBase {
    @Override
    protected String path() {
        return "/vm";
    }
}
