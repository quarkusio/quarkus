package io.quarkus.test.kubernetes.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.HasMetadata;

public abstract class AbstractKubernetesFixturesTestResource extends
        AbstractNamespaceCreatingTestResource {
    private List<HasMetadata> resourceFixtures = Collections.emptyList();
    private int secondsToWaitForFixturesReadiness;
    private final LoggingVisitor deleteLogger = new LoggingVisitor("Deleting");
    private final LoggingVisitor createLogger = new LoggingVisitor("Creating");

    @Override
    protected Map<String, String> doStart() {
        if (!resourceFixtures.isEmpty()) {
            final var resources = client()
                    .resourceList(resourceFixtures)
                    .accept(HasMetadata.class, createLogger);
            resources.createOrReplace();
            resources.waitUntilReady(secondsToWaitForFixturesReadiness, TimeUnit.SECONDS);
        }

        return Collections.emptyMap();
    }

    @Override
    protected void doStop(boolean createdNamespaceWasDeleted) {
        if (resourceFixtures.isEmpty()) {
            return;
        }

        // if the configured namespace (used by fixtures that don't specify one) was deleted, we only need to clean up out of configured namespace resources
        var fixtures = resourceFixtures;
        final var ns = namespace();
        if (createdNamespaceWasDeleted) {
            fixtures = fixtures.stream()
                    .filter(r -> !ns.equals(r.getMetadata().getNamespace()))
                    .collect(Collectors.toList());
        }
        client().resourceList(fixtures)
                .accept(deleteLogger)
                .delete();
    }

    protected void initFixturesWithOptions(List<HasMetadata> resourceFixtures, int fixturesReadinessTimeout, String namespace,
            boolean createNSIfNeeded, boolean preserveNSOnError, int nsDeletionTimeout) {
        if (!resourceFixtures.isEmpty()) {
            initNamespaceAndClient(namespace);
            initNamespaceOptions(createNSIfNeeded, preserveNSOnError, nsDeletionTimeout);
            this.resourceFixtures = resourceFixtures;
            this.secondsToWaitForFixturesReadiness = fixturesReadinessTimeout;
        }
    }

    private class LoggingVisitor implements Visitor<HasMetadata> {
        private final String operation;

        private LoggingVisitor(String operation) {
            this.operation = operation;
        }

        @Override
        public void visit(HasMetadata element) {
            final var metadata = element.getMetadata();
            // if a resource doesn't specify a namespace, use the (potentially created) configured one
            final var ns = metadata.getNamespace() != null ? metadata.getNamespace() : namespace();
            logger().info("{} '{}' {} in namespace '{}'", operation, metadata.getName(), element.getKind(), ns);
        }
    }
}
