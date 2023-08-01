package io.quarkus.devui.runtime.build;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class BuildMetricsJsonRPCService {

    public BuildExecutionMetrics getThreadSlotRecords() {
        BuildExecutionMetrics buildExecutionMetrics = new BuildExecutionMetrics();
        Map<String, Object> buildStepMetrics = buildStepMetrics();
        buildExecutionMetrics.threadSlotRecords = (Map<String, JsonArray>) buildStepMetrics.get("threadSlotRecords");
        buildExecutionMetrics.slots = (List) buildStepMetrics.get("slots");
        return buildExecutionMetrics;
    }

    public JsonArray getBuildItems() {
        Map<String, Object> buildStepMetrics = buildStepMetrics();
        return (JsonArray) buildStepMetrics.get("items");
    }

    public BuildMetrics getBuildMetrics() {
        BuildMetrics buildMetrics = new BuildMetrics();
        Map<String, Object> buildStepMetrics = buildStepMetrics();

        JsonArray records = (JsonArray) buildStepMetrics.get("records");
        Map threadSlotRecords = (Map) buildStepMetrics.get("threadSlotRecords");
        Long duration = (Long) buildStepMetrics.get("duration");

        buildMetrics.numberOfThreads = threadSlotRecords.size();
        buildMetrics.duration = duration;
        buildMetrics.records = records;

        return buildMetrics;
    }

    public JsonObject getDependencyGraph(String buildStepId) {
        Map<String, Object> buildStepMetrics = buildStepMetrics();
        Map<String, JsonObject> dependencyGraphs = (Map<String, JsonObject>) buildStepMetrics.get("dependencyGraphs");

        if (dependencyGraphs.containsKey(buildStepId)) {
            return dependencyGraphs.get(buildStepId);
        }
        return null;
    }

    private Map<String, Object> buildStepMetrics() {
        BuildMetricsDevUIController controller = BuildMetricsDevUIController.get();
        return controller.getBuildStepsMetrics();
    }

    static class BuildMetrics {
        public int numberOfThreads;
        public Long duration;
        public JsonArray records;
    }

    static class BuildExecutionMetrics {
        public List<Long> slots;
        public Map<String, JsonArray> threadSlotRecords;
    }
}
