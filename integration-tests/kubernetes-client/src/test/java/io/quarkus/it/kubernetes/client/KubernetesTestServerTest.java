package io.quarkus.it.kubernetes.client;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@TestProfile(KubernetesTestServerTest.MyProfile.class)
@WithKubernetesTestServer(https = false, crud = true, port = 10001)
@QuarkusTest
public class KubernetesTestServerTest {

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    public void testConfiguration() throws InterruptedException {
        // we can't really test CRUD, and HTTPS doesn't work
        Assertions.assertEquals(10001, mockServer.getMockServer().getPort());
    }

    public static class MyProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> overrides = new HashMap<>();
            // do not fetch config from kubernetes
            overrides.put("quarkus.kubernetes-config.enabled", "false");
            overrides.put("quarkus.kubernetes-config.secrets.enabled", "false");
            // get rid of errors due to us not populating config from kubernetes
            overrides.put("dummy", "asd");
            overrides.put("some.prop1", "asd");
            overrides.put("some.prop2", "asd");
            overrides.put("some.prop3", "asd");
            overrides.put("some.prop4", "asd");
            overrides.put("some.prop5", "asd");
            overrides.put("secret.prop1", "asd");
            overrides.put("secret.prop2", "asd");
            overrides.put("secret.prop3", "asd");
            overrides.put("secret.prop4", "asd");
            overrides.put("overridden.secret", "asd");
            overrides.put("dummysecret", "asd");
            return overrides;
        }
    }
}
