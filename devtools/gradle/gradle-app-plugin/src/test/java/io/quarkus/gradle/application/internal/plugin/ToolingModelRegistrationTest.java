package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.tooling.provider.model.UnknownModelException;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelResolutionViews;

class ToolingModelRegistrationTest {

    @Test
    void standalonePluginOwnsApplicationModelAndSidecar() {
        RecordingRegistry registry = new RecordingRegistry();

        PluginInternal.registerToolingModelsIfOwned(false, registry, mock(ApplicationModelResolutionViews.class));

        assertThat(registry.buildersFor(ApplicationModel.class.getName())).hasSize(1);
        assertThat(registry.buildersFor(GradleApplicationModelSidecar.class.getName())).hasSize(1);
        assertThat(registry.builders).hasSize(2);
    }

    @Test
    void legacyFirstKeepsLegacyApplicationModelOwnerAndRegistersNoSidecar() {
        RecordingRegistry registry = new RecordingRegistry();
        ToolingModelBuilder legacyApplicationModelBuilder = new FixedModelBuilder(ApplicationModel.class.getName());
        registry.register(legacyApplicationModelBuilder);

        PluginInternal.registerToolingModelsIfOwned(true, registry, mock(ApplicationModelResolutionViews.class));

        assertThat(registry.buildersFor(ApplicationModel.class.getName()))
                .containsExactly(legacyApplicationModelBuilder);
        assertThat(registry.buildersFor(GradleApplicationModelSidecar.class.getName())).isEmpty();
        assertThat(registry.builders).containsExactly(legacyApplicationModelBuilder);
    }

    private static final class RecordingRegistry implements ToolingModelBuilderRegistry {
        private final List<ToolingModelBuilder> builders = new ArrayList<>();

        @Override
        public void register(ToolingModelBuilder builder) {
            builders.add(builder);
        }

        @Override
        public ToolingModelBuilder getBuilder(String modelName) {
            return builders.stream()
                    .filter(builder -> builder.canBuild(modelName))
                    .findFirst()
                    .orElseThrow(() -> new UnknownModelException("No builder for " + modelName));
        }

        private List<ToolingModelBuilder> buildersFor(String modelName) {
            return builders.stream().filter(builder -> builder.canBuild(modelName)).toList();
        }
    }

    private record FixedModelBuilder(String modelName) implements ToolingModelBuilder {
        @Override
        public boolean canBuild(String requestedModelName) {
            return modelName.equals(requestedModelName);
        }

        @Override
        public Object buildAll(String requestedModelName, Project project) {
            throw new UnsupportedOperationException();
        }
    }
}
