package io.quarkus.deployment.cmd;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.pkg.builditem.BuildAotOptimizedContainerImageResultBuildItem;

/**
 * Used by the build tool to consume the result of the Quarkus augmentation with the AOT file data
 */
public class BuildEnhancedAotContainerImageCommandHandler implements BiConsumer<Object, BuildResult> {

    public static final String SUCCESS = "success";
    public static final String CONTAINER_IMAGE = "container.image";

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
