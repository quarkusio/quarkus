package io.quarkus.deployment.cmd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.quarkus.builder.BuildChain;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildException;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.builder.ChainBuildException;
import io.quarkus.deployment.pkg.builditem.BuildAotOptimizedContainerImageRequestBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveKind;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;

class BuildAotEnhancedCustomizerProducerTest {

    @Test
    void mapsLegacyContextToAotRequest() throws Exception {
        Path archive = Path.of("app.aot");

        var request = request(Map.of(
                "original-container-image", "quay.io/acme/example:1.0",
                "container-working-directory", "/work",
                "aot-file", archive));

        assertThat(request.getArchiveType()).isEqualTo(JvmStartupOptimizerArchiveType.AOT);
        assertThat(request.getArtifactKind()).isEqualTo(JvmStartupOptimizerArchiveKind.FILE);
        assertThat(request.getArchive()).isEqualTo(archive);
        assertThat(request.getAotFile()).isEqualTo(archive);
    }

    @Test
    void mapsTypedStringContextToSccRequest() throws Exception {
        Path archive = Path.of("app-scc");

        var request = request(Map.of(
                "original-container-image", "quay.io/acme/example:1.0",
                "container-working-directory", "/work",
                "startup-archive-type", "SCC",
                "startup-archive", archive));

        assertThat(request.getArchiveType()).isEqualTo(JvmStartupOptimizerArchiveType.SCC);
        assertThat(request.getArtifactKind()).isEqualTo(JvmStartupOptimizerArchiveKind.DIRECTORY);
        assertThat(request.getArchive()).isEqualTo(archive);
    }

    @Test
    void mapsTypedEnumContext() throws Exception {
        var request = request(Map.of(
                "original-container-image", "quay.io/acme/example:1.0",
                "container-working-directory", "/work",
                "startup-archive-type", JvmStartupOptimizerArchiveType.AppCDS,
                "startup-archive", Path.of("app-cds.jsa")));

        assertThat(request.getArchiveType()).isEqualTo(JvmStartupOptimizerArchiveType.AppCDS);
    }

    @Test
    void acceptsEquivalentLegacyAndTypedContexts() throws Exception {
        Path archive = Path.of("app.aot");
        var context = new HashMap<String, Object>();
        context.put("original-container-image", "quay.io/acme/example:1.0");
        context.put("container-working-directory", "/work");
        context.put("aot-file", archive);
        context.put("startup-archive-type", "AOT");
        context.put("startup-archive", archive);

        assertThat(request(context).getArchiveType()).isEqualTo(JvmStartupOptimizerArchiveType.AOT);
    }

    @Test
    void rejectsPartialTypedContext() {
        assertThatThrownBy(() -> new BuildAotEnhancedCustomizerProducer().apply(Map.of(
                "original-container-image", "quay.io/acme/example:1.0",
                "container-working-directory", "/work",
                "startup-archive-type", "SCC")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both 'startup-archive' and 'startup-archive-type'");
    }

    @Test
    void rejectsConflictingLegacyAndTypedContexts() {
        assertThatThrownBy(() -> new BuildAotEnhancedCustomizerProducer().apply(Map.of(
                "original-container-image", "quay.io/acme/example:1.0",
                "container-working-directory", "/work",
                "aot-file", Path.of("app.aot"),
                "startup-archive-type", "SCC",
                "startup-archive", Path.of("app-scc"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conflicting archives");
    }

    @Test
    void rejectsUnknownArchiveType() {
        assertThatThrownBy(() -> new BuildAotEnhancedCustomizerProducer().apply(Map.of(
                "original-container-image", "quay.io/acme/example:1.0",
                "container-working-directory", "/work",
                "startup-archive-type", "UNKNOWN",
                "startup-archive", Path.of("archive"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported 'startup-archive-type' value 'UNKNOWN'");
    }

    private static BuildAotOptimizedContainerImageRequestBuildItem request(Map<String, Object> context)
            throws ChainBuildException, BuildException {
        var customizers = new BuildAotEnhancedCustomizerProducer().apply(context);
        BuildChainBuilder chainBuilder = BuildChain.builder();
        for (Consumer<BuildChainBuilder> customizer : customizers.getKey()) {
            customizer.accept(chainBuilder);
        }
        chainBuilder.addFinal(BuildAotOptimizedContainerImageRequestBuildItem.class);

        BuildExecutionBuilder executionBuilder = chainBuilder.build().createExecutionBuilder("startup-optimized-image");
        for (Consumer<BuildExecutionBuilder> customizer : customizers.getValue()) {
            customizer.accept(executionBuilder);
        }
        BuildResult result = executionBuilder.execute();
        return result.consume(BuildAotOptimizedContainerImageRequestBuildItem.class);
    }
}
