package io.quarkus.deployment.cmd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.quarkus.builder.BuildChain;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildException;
import io.quarkus.builder.BuildResult;
import io.quarkus.builder.ChainBuildException;
import io.quarkus.deployment.pkg.builditem.BuildAotOptimizedContainerImageResultBuildItem;

class BuildEnhancedAotContainerImageCommandHandlerTest {

    @Test
    void acceptsNullContextForLegacyBuildToolCallers() throws Exception {
        BuildResult result = buildResult("quarkus/example:1.0");

        assertThatCode(() -> new BuildEnhancedAotContainerImageCommandHandler().accept(null, result))
                .doesNotThrowAnyException();
    }

    @Test
    void reportsContainerImageToContextConsumer() throws Exception {
        BuildResult result = buildResult("quarkus/example:1.0");
        AtomicReference<Map<String, String>> handlerResult = new AtomicReference<>();
        Consumer<Map<String, String>> resultConsumer = handlerResult::set;

        new BuildEnhancedAotContainerImageCommandHandler().accept(resultConsumer, result);

        assertThat(handlerResult.get())
                .containsEntry(BuildEnhancedAotContainerImageCommandHandler.SUCCESS, "true")
                .containsEntry(BuildEnhancedAotContainerImageCommandHandler.CONTAINER_IMAGE, "quarkus/example:1.0");
    }

    private static BuildResult buildResult(String containerImage) throws ChainBuildException, BuildException {
        BuildChainBuilder builder = BuildChain.builder();
        builder.addBuildStep((BuildContext context) -> context
                .produce(new BuildAotOptimizedContainerImageResultBuildItem(containerImage)))
                .produces(BuildAotOptimizedContainerImageResultBuildItem.class)
                .build();
        builder.addFinal(BuildAotOptimizedContainerImageResultBuildItem.class);
        return builder.build()
                .createExecutionBuilder("aot-enhanced-image")
                .execute();
    }
}
