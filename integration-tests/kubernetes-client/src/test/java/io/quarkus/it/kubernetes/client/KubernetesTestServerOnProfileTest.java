package io.quarkus.it.kubernetes.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

/*
 * This class has no native-image test because it relies on setting config overrides that clash
 * with native image build config.
 * This is the same test as KubernetesTestServerTest but with the test resource annotation on the profile
 */
@TestProfile(KubernetesTestServerOnProfileTest.MyProfile.class)
@QuarkusTest
public class KubernetesTestServerOnProfileTest {

    private static KubernetesServer setupServer;

    public static class Setup implements Consumer<KubernetesServer> {

        @Override
        public void accept(KubernetesServer t) {
            setupServer = t;
        }

    }

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    public void testConfiguration() {
        // we can't really test CRUD, and HTTPS doesn't work
        Assertions.assertEquals(10001, mockServer.getKubernetesMockServer().getPort());
        Assertions.assertSame(mockServer, setupServer);
    }

    @WithKubernetesTestServer(https = false, crud = true, port = 10001, setup = KubernetesTestServerOnProfileTest.Setup.class)
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
