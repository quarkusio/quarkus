package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;
import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.slf4j.Logger;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class AbstractNamespaceManagingTestResource implements
        QuarkusTestResourceLifecycleManager {

    private String namespace;
    private KubernetesClient client;

    private boolean createdNamespace = false;
    private Context context;
    //    private boolean preserveNamespaceOnError; todo: ns preservation

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public Map<String, String> start() {
        createNamespaceIfRequested();
        createdNamespace = true;

        return doStart();
    }

    @Override
    public void stop() {
        if (createdNamespace) {
            // todo: add namespace preservation on error if/when quarkusio/quarkus#25905 becomes available
            logger().info("Deleting namespace '{}'", namespace);
            client.namespaces().withName(namespace).delete();
            final var secondsToWaitForNamespaceDeletion = numberOfSecondsToWaitForNamespaceDeletion();
            if (secondsToWaitForNamespaceDeletion > 0) {
                logger().info("Waiting for namespace '{}' to be deleted", namespace);
                Awaitility.await("namespace deleted")
                        .pollInterval(50, TimeUnit.MILLISECONDS)
                        .atMost(secondsToWaitForNamespaceDeletion, TimeUnit.SECONDS)
                        .until(() -> client.namespaces().withName(namespace).get() == null);
            }
        }

        // todo: right now, we need clean up if we didn't create the namespace (and therefore, deleted it above)
        // however when namespace preservation is available, this logic will need to change
        doStop(!createdNamespace);
        client.close();
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

    protected void createNamespaceIfRequested() {
        final var configuration = client.getConfiguration();
        logger().info("Connecting to cluster {}", configuration.getMasterUrl());

        if (client.namespaces().withName(namespace).get() != null) {
            logger().info("Namespace '{}' already exists", namespace);
            return;
        }

        if (shouldCreateNamespace()) {
            logger().info("Creating '{}' namespace", namespace);
            try {
                client.namespaces()
                        .create(new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata()
                                .build());
            } catch (KubernetesClientException e) {
                if (e.getCause() instanceof ConnectException) {
                    logger().error(
                            "Couldn't connect to context '{}'. Tests annotated with @{} require a running Kubernetes cluster.",
                            configuration.getCurrentContext().getName(),
                            relatedAnnotationClass().getSimpleName());
                }
                throw e;
            }
        }
    }

    protected boolean shouldCreateNamespace() {
        return true;
    }

    protected int numberOfSecondsToWaitForNamespaceDeletion() {
        return 0;
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

    protected abstract Map<String, String> doStart();

    protected abstract void doStop(boolean deletedNamespace);

    protected abstract Class<? extends Annotation> relatedAnnotationClass();

    protected abstract Logger logger();
}
