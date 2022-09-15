package io.quarkus.grpc.runtime.health;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import grpc.health.v1.HealthOuterClass.HealthCheckResponse.ServingStatus;

/**
 * Eclipse MicroProfile Health Check
 */
@Readiness
@ApplicationScoped
public class GrpcHealthCheck implements HealthCheck {

    @Inject
    GrpcHealthStorage healthService;

    @Override
    public HealthCheckResponse call() {
        ServingStatus servingStatus = healthService.getStatuses().get(GrpcHealthStorage.DEFAULT_SERVICE_NAME);

        HealthCheckResponseBuilder builder = HealthCheckResponse.named("gRPC Server health check").up();
        builder.name("gRPC Server");

        if (isUp(servingStatus)) {
            builder.up();
        } else {
            builder.down();
        }

        for (Map.Entry<String, ServingStatus> statusEntry : healthService.getStatuses().entrySet()) {
            String serviceName = statusEntry.getKey();
            if (!serviceName.equals(GrpcHealthStorage.DEFAULT_SERVICE_NAME)) {
                builder.withData(serviceName, isUp(statusEntry.getValue()));
            }
        }

        return builder.build();
    }

    private boolean isUp(ServingStatus value) {
        return value == ServingStatus.SERVING;
    }
}
