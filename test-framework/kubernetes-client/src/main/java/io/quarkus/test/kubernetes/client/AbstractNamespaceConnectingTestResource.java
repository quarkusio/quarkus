package io.quarkus.test.kubernetes.client;

import org.slf4j.Logger;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class AbstractNamespaceConnectingTestResource implements
        QuarkusTestResourceLifecycleManager {

    private String namespace;
    private KubernetesClient client;

    @Override
    public int order() {
        // if WithDisposableNamespace is used, make sure we start after it so that we use the same
        // namespace if we're not creating the resources in a specific namespace
        return 1;
    }

    protected void initNamespaceAndClient(String optionalNamespace) {
        final var defaultKubernetesClient = new DefaultKubernetesClient();

        var namespace = optionalNamespace;
        if (namespace == null || AnnotationConstants.UNSET_STRING_VALUE.equals(namespace)) {
            // connect to the namespace configured by default but let subclasses the opportunity to decide otherwise
            namespace = defaultNamespaceName(defaultKubernetesClient);
        }
        this.namespace = namespace;

        client = defaultKubernetesClient.inNamespace(namespace);
    }

    protected String namespace() {
        return namespace;
    }

    protected KubernetesClient client() {
        return client;
    }

    protected String defaultNamespaceName(KubernetesClient client) {
        return client.getNamespace();
    }

    protected abstract Logger logger();
}
