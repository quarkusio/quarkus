package io.quarkus.kubernetes.client.test.utils;

import java.util.Map;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.common.DevServicesContext;

/**
 * Builds a {@link KubernetesClient} pointing at the cluster started by the Kubernetes Client Dev Services,
 * from the connection properties exposed via {@link DevServicesContext}.
 * <p>
 * Useful in {@code @QuarkusIntegrationTest} (including native tests), where the tested artifact runs in a
 * separate process, so a client can't be obtained via CDI injection like in a {@code @QuarkusTest}.
 */
public final class KubernetesClientDevServicesUtils {

    private static final String KUBERNETES_CLIENT_MASTER_URL = "quarkus.kubernetes-client.api-server-url";
    private static final String KUBERNETES_CLIENT_CA_CERT_DATA = "quarkus.kubernetes-client.ca-cert-data";
    private static final String KUBERNETES_CLIENT_CLIENT_CERT_DATA = "quarkus.kubernetes-client.client-cert-data";
    private static final String KUBERNETES_CLIENT_CLIENT_KEY_DATA = "quarkus.kubernetes-client.client-key-data";
    private static final String KUBERNETES_CLIENT_CLIENT_KEY_ALGO = "quarkus.kubernetes-client.client-key-algo";
    private static final String KUBERNETES_CLIENT_NAMESPACE = "quarkus.kubernetes-client.namespace";

    private KubernetesClientDevServicesUtils() {
    }

    public static KubernetesClient createClient(DevServicesContext context) {
        return new KubernetesClientBuilder().withConfig(createConfig(context)).build();
    }

    public static Config createConfig(DevServicesContext context) {
        Map<String, String> props = context.devServicesProperties();

        return new ConfigBuilder()
                .withMasterUrl(props.get(KUBERNETES_CLIENT_MASTER_URL))
                .withCaCertData(props.get(KUBERNETES_CLIENT_CA_CERT_DATA))
                .withClientCertData(props.get(KUBERNETES_CLIENT_CLIENT_CERT_DATA))
                .withClientKeyData(props.get(KUBERNETES_CLIENT_CLIENT_KEY_DATA))
                .withClientKeyAlgo(props.get(KUBERNETES_CLIENT_CLIENT_KEY_ALGO))
                .withNamespace(props.get(KUBERNETES_CLIENT_NAMESPACE))
                .build();
    }
}
