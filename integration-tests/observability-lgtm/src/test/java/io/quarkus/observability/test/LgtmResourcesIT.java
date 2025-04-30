package io.quarkus.observability.test;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(LgtmResourcesTest.DevResourcesTestProfileOnly.class)
@DisabledOnOs(OS.WINDOWS)
public class LgtmResourcesIT extends LgtmTestBase {
    @Override
    protected String grafanaEndpoint() {
        return ConfigProvider.getConfig().getValue("grafana.endpoint", String.class);
    }
}
