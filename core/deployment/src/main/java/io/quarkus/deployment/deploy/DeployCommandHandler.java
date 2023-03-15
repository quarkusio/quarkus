package io.quarkus.deployment.deploy;

import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;

public class DeployCommandHandler implements BiConsumer<Map<String, String>, BuildResult> {

    public static final String KIND = "kind";
    public static final String NAME = "name";
    public static final String URL = "url";

    @Override
    public void accept(Map<String, String> context, BuildResult result) {
        DeploymentResultBuildItem deploymentResult = result.consume(DeploymentResultBuildItem.class);
        context.put(KIND, deploymentResult.getKind());
        context.put(NAME, deploymentResult.getName());
        deploymentResult.getUrl().ifPresent(url -> context.put(URL, url));
    }
}
