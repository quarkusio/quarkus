package io.quarkus.deployment.cmd;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.builder.BuildResult;

public class DeployCommandDeclarationHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object o, BuildResult buildResult) {
        DeployCommandDeclarationResultBuildItem result = buildResult.consume(DeployCommandDeclarationResultBuildItem.class);

        // FYI: AugmentAction.performCustomBuild runs in its own classloader
        // so we can only pass back instances of those classes in the system classloader

        Consumer<List<String>> consumer = (Consumer<List<String>>) o;
        consumer.accept(result.getCommands());
    }
}
