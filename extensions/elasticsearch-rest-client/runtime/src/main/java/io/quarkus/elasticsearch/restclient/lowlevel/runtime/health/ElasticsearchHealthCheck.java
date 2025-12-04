package io.quarkus.elasticsearch.restclient.lowlevel.runtime.health;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.All;
import io.quarkus.arc.InstanceHandle;

@Readiness
@ApplicationScoped
public class ElasticsearchHealthCheck implements HealthCheck {
    @Inject
    @All
    List<InstanceHandle<ElasticsearchHealthCheckCondition>> conditionHandles;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Elasticsearch cluster health check").up();
        boolean isUp = true;
        for (InstanceHandle<ElasticsearchHealthCheckCondition> handle : conditionHandles) {
            if (!handle.getBean().isActive()) {
                continue;
            }
            ElasticsearchHealthCheckCondition.Status status = handle.get().check();
            if (status.reason() != null || "red".equals(status.status())) {
                isUp = false;
            }
            builder.withData("status(%s)".formatted(status.clientName()), status.status());
            builder.withData("reason(%s)".formatted(status.clientName()), status.reason());
        }
        builder.status(isUp);
        return builder.build();
    }
}
