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
    private boolean preserveNamespaceOnError = false;
    private int secondsToWaitForNamespaceDeletion = 0;
    private boolean shouldCreateNamespace = true;

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public Map<String, String> start() {
        createdNamespace = createNamespaceIfRequested();
        return doStart();
    }

    @Override
    public void stop() {
        final var client = client();

        boolean deleted = false;
        // we preserve the namespace only if it was requested and the test failed
        if (createdNamespace && !(preserveNamespaceOnError && context.getTestStatus().isTestFailed())) {
            final var namespace = namespace();
            logger().info("Deleting namespace '{}'", namespace);
            try {
                client.namespaces().withName(namespace).delete();
                deleted = true;
            } catch (Exception e) {
                logger().warn("Couldn't delete namespace '" + namespace + "'", e);
            }

            if (deleted) {
                if (secondsToWaitForNamespaceDeletion > 0) {
                    logger().info("Waiting for namespace '{}' to be deleted", namespace);
                    Awaitility.await("namespace deleted")
                            .pollInterval(250, TimeUnit.MILLISECONDS)
                            .atMost(secondsToWaitForNamespaceDeletion, TimeUnit.SECONDS)
                            .until(() -> client.namespaces().withName(namespace).get() == null);
                }
            }
        }

        // let subclasses the opportunity to clean up out of namespace resources they might have created
        doStop(!deleted);
        client.close();
    }

    protected boolean createNamespaceIfRequested() {
        final var client = client();
        final var configuration = client.getConfiguration();
        final var namespace = namespace();
        logger().info("Connecting to cluster {}", configuration.getMasterUrl());

        if (client.namespaces().withName(namespace).get() != null) {
            logger().info("Namespace '{}' already exists", namespace);
            return false;
        }

        if (shouldCreateNamespace) {
            logger().info("Creating '{}' namespace", namespace);
            try {
                client.namespaces()
                        .create(new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata()
                                .build());
                return true;
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

        return false;
    }

    protected void initNamespaceOptions(boolean shouldCreateNamespace, boolean preserveNamespaceOnError,
            int secondsToWaitForNamespaceDeletion) {
        this.preserveNamespaceOnError = preserveNamespaceOnError;
        this.shouldCreateNamespace = shouldCreateNamespace;
        this.secondsToWaitForNamespaceDeletion = secondsToWaitForNamespaceDeletion;
    }

    protected abstract Map<String, String> doStart();

    protected abstract void doStop(boolean deletedNamespace);

    protected abstract Class<? extends Annotation> relatedAnnotationClass();
}
