package io.quarkus.deployment.cmd;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.deployment.pkg.builditem.BuildAotOptimizedContainerImageRequestBuildItem;

/**
 * Used by the build tool to customize a Quarkus augmentation with the AOT file data
 */
public class BuildAotEnhancedCustomizerProducer implements
        Function<Map<String, Object>, Map.Entry<List<Consumer<BuildChainBuilder>>, List<Consumer<BuildExecutionBuilder>>>> {

    @Override
    public Map.Entry<List<Consumer<BuildChainBuilder>>, List<Consumer<BuildExecutionBuilder>>> apply(
            Map<String, Object> context) {
        String originalContainerImage = (String) context.get("original-container-image");
        String containerWorkingDirectory = (String) context.get("container-working-directory");
        Path aotFile = (Path) context.get("aot-file");
        return new AbstractMap.SimpleEntry<>(List.of(new BuildChainCustomizer()),
                List.of(new BuildExecutionCustomizer(originalContainerImage, containerWorkingDirectory, aotFile)));
    }

    public static class BuildChainCustomizer implements Consumer<BuildChainBuilder> {

        @Override
        public void accept(BuildChainBuilder buildChainBuilder) {
            buildChainBuilder.addInitial(BuildAotOptimizedContainerImageRequestBuildItem.class);
        }
    }

    public static class BuildExecutionCustomizer implements Consumer<BuildExecutionBuilder> {

        private final String originalContainerImage;
        private final String containerWorkingDirectory;
        private final Path aotFile;

        public BuildExecutionCustomizer(String originalContainerImage, String containerWorkingDirectory, Path aotFile) {
            this.originalContainerImage = originalContainerImage;
            this.containerWorkingDirectory = containerWorkingDirectory;
            this.aotFile = aotFile;
        }

        @Override
        public void accept(BuildExecutionBuilder buildExecutionBuilder) {
            buildExecutionBuilder.produce(new BuildAotOptimizedContainerImageRequestBuildItem(originalContainerImage,
                    containerWorkingDirectory, aotFile));
        }
    }
}
