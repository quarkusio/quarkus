package io.quarkus.deployment.cmd;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.builder.BuildResult;

public class DeployCommandHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object o, BuildResult buildResult) {
        DeployCommandActionResultBuildItem result = buildResult.consume(DeployCommandActionResultBuildItem.class);

        // FYI: AugmentAction.performCustomBuild runs in its own classloader
        // so we can only pass back instances of those classes in the system classloader

        Consumer<Boolean> consumer = (Consumer<Boolean>) o;
        if (result.getCommands().isEmpty()) {
            consumer.accept(false);
        } else {
            consumer.accept(true);
        }
    }
}
