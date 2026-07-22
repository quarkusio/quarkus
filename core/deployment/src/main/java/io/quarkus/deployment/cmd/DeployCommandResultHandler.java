package io.quarkus.deployment.cmd;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;

public final class DeployCommandResultHandler implements BiConsumer<Object, BuildResult> {

    public static final String SUCCESS = "success";
    public static final String RESULT_NAME = "result.name";
    public static final String RESULT_LABEL_PREFIX = "result.labels.";

    @Override
    @SuppressWarnings("unchecked")
    public void accept(Object context, BuildResult buildResult) {
        DeployCommandActionResultBuildItem actionResult = buildResult.consume(DeployCommandActionResultBuildItem.class);
        DeploymentResultBuildItem deploymentResult = buildResult.consume(DeploymentResultBuildItem.class);

        Map<String, String> result = new LinkedHashMap<>();
        result.put(SUCCESS, Boolean.toString(actionResult != null && !actionResult.getCommands().isEmpty()));
        if (deploymentResult != null) {
            String name = deploymentResult.getName();
            if (name != null && !name.isBlank()) {
                result.put(RESULT_NAME, name);
            }
            Map<String, String> labels = deploymentResult.getLabels() == null ? Map.of() : deploymentResult.getLabels();
            labels.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> result.put(RESULT_LABEL_PREFIX + entry.getKey(), entry.getValue()));
        }

        ((Consumer<Map<String, String>>) context).accept(result);
    }
}
