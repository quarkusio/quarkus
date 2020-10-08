package io.quarkus.test.kubernetes.client;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.EventList;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.ServiceAccountList;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressList;
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
        systemProps.put(Config.KUBERNETES_HTTP2_DISABLE, "true");

        mockServer = createMockServer();
        mockServer.init();
        try (NamespacedKubernetesClient client = mockServer.createClient()) {
            systemProps.put(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, client.getConfiguration().getMasterUrl());
        }

        configureMockServer(mockServer);

        Optional<Boolean> defaultTypes = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.kubernetes-client.test.default-types", Boolean.class);
        if (defaultTypes.orElse(false)) {
            registerDefaultTypes(mockServer);
        }

        return systemProps;
    }

    protected KubernetesMockServer createMockServer() {
        return new KubernetesMockServer(useHttps());
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

    protected boolean useHttps() {
        return Boolean.getBoolean("quarkus.kubernetes-client.test.https");
    }

    private void registerDefaultTypes(KubernetesMockServer mockServer) {
        final String ns = System.getProperty("quarkus.kubernetes-client.namespace", "test");
        final String basePath = "/api/v1/namespaces/" + ns;

        mockServer.expect().get().withPath(basePath + "/configmaps")
                .andReturn(200, new ConfigMapList())
                .always();
        mockServer.expect().get().withPath(basePath + "/configmaps?watch=true")
                .andReturn(200, new ConfigMapList())
                .always();

        mockServer.expect().get().withPath("/apis/apps/v1/namespaces/" + ns + "/deployments")
                .andReturn(200, new DeploymentList())
                .always();
        mockServer.expect().get().withPath("/apis/apps/v1/namespaces/" + ns + "/deployments?watch=true")
                .andReturn(200, new DeploymentList())
                .always();

        mockServer.expect().get().withPath(basePath + "/events")
                .andReturn(200, new EventList())
                .always();
        mockServer.expect().get().withPath(basePath + "/events?watch=true")
                .andReturn(200, new EventList())
                .always();

        mockServer.expect().get().withPath("/apis/extensions/v1beta1/namespaces/" + ns + "/ingresses")
                .andReturn(200, new IngressList())
                .always();
        mockServer.expect().get().withPath("/apis/extensions/v1beta1/namespaces/" + ns + "/ingresses?watch=true")
                .andReturn(200, new IngressList())
                .always();

        mockServer.expect().get().withPath(basePath + "/pods")
                .andReturn(200, new PodList())
                .always();
        mockServer.expect().get().withPath(basePath + "/pods?watch=true")
                .andReturn(200, new PodList())
                .always();

        mockServer.expect().get().withPath(basePath + "/secrets")
                .andReturn(200, new SecretList())
                .always();
        mockServer.expect().get().withPath(basePath + "/secrets?watch=true")
                .andReturn(200, new SecretList())
                .always();

        mockServer.expect().get().withPath(basePath + "/serviceaccounts")
                .andReturn(200, new ServiceAccountList())
                .always();
        mockServer.expect().get().withPath(basePath + "/serviceaccounts?watch=true")
                .andReturn(200, new ServiceAccountList())
                .always();

        mockServer.expect().get().withPath(basePath + "/services")
                .andReturn(200, new ServiceList())
                .always();
        mockServer.expect().get().withPath(basePath + "/services?watch=true")
                .andReturn(200, new ServiceList())
                .always();
    }
}
