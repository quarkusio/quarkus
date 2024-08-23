package io.quarkus.devtools.project.update.rewrite;

import java.util.List;
import java.util.Map;

import io.quarkus.devtools.project.BuildTool;

/**
 * A rewrite operation to be used in a QuarkusUpdateRecipe
 */
public interface RewriteOperation {

    /**
     * Return the OpenRewrite operation name and the parameters as a map
     *
     * { "operation-name": { "p1": "v1" }}
     *
     * @param buildTool
     * @return
     */
    default Map<String, Object> single(BuildTool buildTool) {
        return Map.of();
    }

    default List<Map<String, Object>> multi(BuildTool buildTool) {
        return List.of(single(buildTool));
    }

}
