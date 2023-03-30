package io.quarkus.devui.runtime.build;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BuildMetricsJsonRPCService {

    public Map<String, Object> getBuildStepsMetrics() {
        return BuildMetricsDevUIController.get().getBuildStepsMetrics();
    }
}
