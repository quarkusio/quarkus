package io.quarkus.observability.test;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.observability.test.utils.GrafanaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisabledOnOs(OS.WINDOWS)
public class LgtmGrafanaDashboardTest {

    @ConfigProperty(name = "grafana.endpoint")
    String endpoint;

    @Test
    public void testCustomGrafanaDashboard() {
        GrafanaClient client = new GrafanaClient(endpoint, "admin", "admin");
        String uid = "Qwerty123";
        String dashboard = client.dashboard(uid);
        Assertions.assertTrue(dashboard.contains(uid));
    }

}
