package io.quarkus.infinispan.client.runtime.health;

import java.util.Arrays;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.infinispan.client.hotrod.RemoteCacheManager;

@Readiness
@ApplicationScoped
public class InfinispanHealthCheck implements HealthCheck {

    @Inject
    RemoteCacheManager cacheManager;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Infinispan cluster health check");
        try {
            if (cacheManager.isStarted()) {
                Set<String> cacheNames = cacheManager.getCacheNames();
                builder.up().withData("servers", Arrays.toString(cacheManager.getServers()))
                        .withData("caches-size", cacheNames.size());
            } else {
                builder.down();
            }
        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
        return builder.build();
    }
}
