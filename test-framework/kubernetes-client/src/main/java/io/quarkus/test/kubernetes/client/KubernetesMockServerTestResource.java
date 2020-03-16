package io.quarkus.test.kubernetes.client;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KubernetesMockServerTestResource implements QuarkusTestResourceLifecycleManager {

    private KubernetesMockServer mockServer;

    @Override
    public Map<String, String> start() {
        final Map<String, String> systemProps = new HashMap<>();
        systemProps.put(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        systemProps.put(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
        systemProps.put(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");
        systemProps.put(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "test");

        mockServer = new KubernetesMockServer(useHttps());
        mockServer.init();
        try (NamespacedKubernetesClient client = mockServer.createClient()) {
            systemProps.put(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, client.getConfiguration().getMasterUrl());
        }

        configureMockServer(mockServer);

        return systemProps;
    }

    /**
     * Can be used by subclasses of {@code KubernetesMockServerTestResource} in order to
     * setup the mock server before the Quarkus application starts
     */
    public void configureMockServer(KubernetesMockServer mockServer) {

    }

    @Override
    public void stop() {
        if (mockServer != null) {
            mockServer.destroy();
        }
    }

    @Override
    public void inject(Object testInstance) {
        Class<?> c = testInstance.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getAnnotation(MockServer.class) != null) {
                    if (!KubernetesMockServer.class.isAssignableFrom(f.getType())) {
                        throw new RuntimeException("@MockServer can only be used on fields of type KubernetesMockServer");
                    }

                    f.setAccessible(true);
                    try {
                        f.set(testInstance, mockServer);
                        return;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    private boolean useHttps() {
        return Boolean.getBoolean("quarkus.kubernetes-client.test.https");
    }
}
