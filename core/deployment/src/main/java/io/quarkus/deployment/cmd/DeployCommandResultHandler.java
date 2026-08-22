package io.quarkus.deployment.cmd;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;

/**
 * Converts deployment build items into the string-valued result consumed by build-tool deployment commands.
 * <p>
 * Success indicates that the deployment action produced at least one command. When the deployment provider also
 * reports a resource name or labels, those values are included in deterministic label-key order.
 */
public final class DeployCommandResultHandler implements BiConsumer<Object, BuildResult> {

    /** Result-map key indicating whether the deployment action produced commands. */
    public static final String SUCCESS = "success";

    /** Result-map key containing the deployed resource name, when reported. */
    public static final String RESULT_NAME = "result.name";

    /** Prefix applied to deployment-result label keys. */
    public static final String RESULT_LABEL_PREFIX = "result.labels.";

    /**
     * Consumes the deployment outcome and sends its mapped representation to the context consumer.
     *
     * @param context a {@code Consumer<Map<String, String>>} that receives the result
     * @param buildResult the completed augmentation result
     */
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
