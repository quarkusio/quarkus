package io.quarkus.test.kubernetes.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.HasMetadata;

public abstract class AbstractKubernetesFixturesTestResource extends
        AbstractNamespaceCreatingTestResource {
    private List<HasMetadata> resourceFixtures = Collections.emptyList();
    private int waitAtMostSecondsForFixturesReadiness;
    private boolean createNSIfNeeded;

    @Override
    protected Map<String, String> doStart() {
        final var resources = client().resourceList(resourceFixtures);
        resources.accept(HasMetadata.class,
                hasMetadata -> logger().info("Creating '{}' {} in namespace '{}'",
                        hasMetadata.getMetadata().getName(),
                        hasMetadata.getKind(), namespace()));
        resources.createOrReplace();
        resources.waitUntilReady(waitAtMostSecondsForFixturesReadiness, TimeUnit.SECONDS);

        return Collections.emptyMap();
    }

    @Override
    protected void doStop(boolean needsCleanup) {
        if (needsCleanup) {
            client().resourceList(resourceFixtures)
                    .accept(HasMetadata.class, hasMetadata -> logger().info("Deleting '{}' {} in namespace '{}'",
                            hasMetadata.getMetadata().getName(),
                            hasMetadata.getKind(), namespace()))
                    .delete();
        }
    }

    protected void initNamespaceAndClient(String optionalNamespace, int readinessTimeoutSeconds,
            boolean createNamespaceIfNeeded) {
        initNamespaceAndClient(optionalNamespace);
        waitAtMostSecondsForFixturesReadiness = readinessTimeoutSeconds;
        createNSIfNeeded = createNamespaceIfNeeded;
    }

    protected void initFixtures(List<HasMetadata> resourceFixtures) {
        this.resourceFixtures = resourceFixtures;
    }

    @Override
    protected boolean shouldCreateNamespace() {
        return createNSIfNeeded;
    }
}
