package io.quarkus.devtools.project.update.rewrite;

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
    Map<String, Object> toMap(BuildTool buildTool);
}
