package io.quarkus.kubernetes.client.runtime.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KubernetesClusterFixtures {
    private static final Logger log = Logger.getLogger(KubernetesClusterFixtures.class.getName());

    public void apply(List<HasMetadata> resources) {
        try (final var clientHandle = Arc.container().instance(KubernetesClient.class)) {
            final var client = clientHandle.get();
            List<HasMetadata> resourcesWithReadiness = new ArrayList<>();
            resources.forEach(resource -> {
                log.infof("Applying %s %s to the cluster",
                        resource.getClass().getSimpleName(),
                        resource.getMetadata().getName());
                client.resource(resource).create();

                if (Readiness.getInstance().isReadinessApplicable(resource)) {
                    resourcesWithReadiness.add(resource);
                }
            });

            resourcesWithReadiness.forEach(resource -> {
                log.infof("Waiting for %s %s to be ready...",
                        resource.getClass().getSimpleName(),
                        resource.getMetadata().getName());
                client.resource(resource).waitUntilReady(60, TimeUnit.SECONDS);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
