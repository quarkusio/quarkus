package io.quarkus.deployment.cmd;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.deployment.pkg.builditem.BuildPgoOptimizedNativeRequestBuildItem;

/**
 * Used by the build tool to customize a Quarkus augmentation with the PGO profile data
 */
public class BuildPgoEnhancedCustomizerProducer implements
        Function<Map<String, Object>, Map.Entry<List<Consumer<BuildChainBuilder>>, List<Consumer<BuildExecutionBuilder>>>> {

    @Override
    public Map.Entry<List<Consumer<BuildChainBuilder>>, List<Consumer<BuildExecutionBuilder>>> apply(
            Map<String, Object> context) {
        Path profilePath = (Path) context.get("pgo-profile");
        return Map.entry(List.of(new BuildChainCustomizer()),
                List.of(new BuildExecutionCustomizer(profilePath)));
    }

    public static class BuildChainCustomizer implements Consumer<BuildChainBuilder> {

        @Override
        public void accept(BuildChainBuilder buildChainBuilder) {
            buildChainBuilder.addInitial(BuildPgoOptimizedNativeRequestBuildItem.class);
        }
    }

    public static class BuildExecutionCustomizer implements Consumer<BuildExecutionBuilder> {

        private final Path profilePath;

        public BuildExecutionCustomizer(Path profilePath) {
            this.profilePath = profilePath;
        }

        @Override
        public void accept(BuildExecutionBuilder buildExecutionBuilder) {
            buildExecutionBuilder.produce(new BuildPgoOptimizedNativeRequestBuildItem(profilePath));
        }
    }
}
