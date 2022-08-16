package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;
import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;

public abstract class AbstractNamespaceCreatingTestResource extends AbstractNamespaceConnectingTestResource {

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
        final var client = client();

        boolean deleted = false;
        if (createdNamespace) {
            // todo: add namespace preservation on error if/when quarkusio/quarkus#25905 becomes available
            final var namespace = namespace();
            logger().info("Deleting namespace '{}'", namespace);
            try {
                deleted = client.namespaces().withName(namespace).delete();
            } catch (Exception e) {
                logger().warn("Couldn't delete namespace '" + namespace + "'", e);
            }

            if (deleted) {
                final var secondsToWaitForNamespaceDeletion = numberOfSecondsToWaitForNamespaceDeletion();
                if (secondsToWaitForNamespaceDeletion > 0) {
                    logger().info("Waiting for namespace '{}' to be deleted", namespace);
                    Awaitility.await("namespace deleted")
                        .pollInterval(50, TimeUnit.MILLISECONDS)
                        .atMost(secondsToWaitForNamespaceDeletion, TimeUnit.SECONDS)
                        .until(() -> client.namespaces().withName(namespace).get() == null);
                }
            }
        }

        // todo: right now, we need clean up if we didn't create the namespace (and therefore, deleted it above)
        // however when namespace preservation is available, this logic will need to change
        doStop(!deleted);
        client.close();
    }

    protected void createNamespaceIfRequested() {
        final var client = client();
        final var configuration = client.getConfiguration();
        final var namespace = namespace();
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

    protected abstract Map<String, String> doStart();

    protected abstract void doStop(boolean deletedNamespace);

    protected abstract Class<? extends Annotation> relatedAnnotationClass();
}
