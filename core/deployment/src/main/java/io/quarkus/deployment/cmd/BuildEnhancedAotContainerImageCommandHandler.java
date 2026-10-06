package io.quarkus.deployment.cmd;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.pkg.builditem.BuildAotOptimizedContainerImageResultBuildItem;

/**
 * Converts startup-optimized container-image build items into the build-tool command result.
 * <p>
 * The result reports success only when augmentation produced exactly one image result. A {@code null} context
 * suppresses result delivery.
 */
public class BuildEnhancedAotContainerImageCommandHandler implements BiConsumer<Object, BuildResult> {

    /** Result-map key whose value indicates whether exactly one image was produced. */
    public static final String SUCCESS = "success";

    /** Result-map key containing the produced container-image reference on success. */
    public static final String CONTAINER_IMAGE = "container.image";

    /**
     * Consumes image results and sends the mapped outcome to the context consumer, when present.
     *
     * @param context a {@code Consumer<Map<String, String>>}, or {@code null} to discard the outcome
     * @param buildResult the completed augmentation result
     */
    @Override
    @SuppressWarnings("unchecked")
    public void accept(Object context, BuildResult buildResult) {
        List<BuildAotOptimizedContainerImageResultBuildItem> resultItems = buildResult
                .consumeMulti(BuildAotOptimizedContainerImageResultBuildItem.class);

        Map<String, String> result = new LinkedHashMap<>();
        if (resultItems.size() == 1) {
            result.put(SUCCESS, "true");
            result.put(CONTAINER_IMAGE, resultItems.get(0).getContainerImage());
        } else {
            result.put(SUCCESS, "false");
        }
        if (context == null) {
            return;
        }
        ((Consumer<Map<String, String>>) context).accept(result);
    }
}
