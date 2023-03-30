package io.quarkus.devui.runtime.build;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BuildStepsJsonRPCService {

    public Map<String, Object> getBuildStepsMetrics() {
        return BuildStepsDevUIController.get().getBuildStepsMetrics();
    }
}
