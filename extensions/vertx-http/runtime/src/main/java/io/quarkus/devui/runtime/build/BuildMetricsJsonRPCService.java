package io.quarkus.devui.runtime.build;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@ApplicationScoped
public class BuildMetricsJsonRPCService {

    public Uni<Map<String, Object>> getBuildStepsMetrics() {
        return Uni.createFrom().item(() -> BuildMetricsDevUIController.get().getBuildStepsMetrics())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
