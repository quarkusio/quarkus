package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveKind;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;

class JvmStartupOptimizerArchiveBuildStepTest {

    @Test
    void publishesHistoricalAppCdsTypeWithTypedMetadata() {
        Path archive = Path.of("app-cds.jsa");

        var result = JvmStartupOptimizerArchiveBuildStep.archiveArtifactResult(
                archive, JvmStartupOptimizerArchiveType.AppCDS);

        assertThat(result.getPath()).isEqualTo(archive);
        assertThat(result.getType()).isEqualTo("appCDS");
        assertThat(result.getMetadata())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "archive-type", JvmStartupOptimizerArchiveType.AppCDS.name(),
                        "artifact-kind", JvmStartupOptimizerArchiveKind.FILE.name()));
    }

    @Test
    void publishesTypedFileArtifactMetadata() {
        Path archive = Path.of("app.aot");

        var result = JvmStartupOptimizerArchiveBuildStep.archiveArtifactResult(
                archive, JvmStartupOptimizerArchiveType.AOT);

        assertThat(result.getPath()).isEqualTo(archive);
        assertThat(result.getType()).isEqualTo("appCDS");
        assertThat(result.getMetadata())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "archive-type", JvmStartupOptimizerArchiveType.AOT.name(),
                        "artifact-kind", JvmStartupOptimizerArchiveKind.FILE.name()));
    }

    @Test
    void publishesTypedDirectoryArtifactMetadata() {
        Path archive = Path.of("app-scc");

        var result = JvmStartupOptimizerArchiveBuildStep.archiveArtifactResult(
                archive, JvmStartupOptimizerArchiveType.SCC);

        assertThat(result.getPath()).isEqualTo(archive);
        assertThat(result.getType()).isEqualTo("appCDS");
        assertThat(result.getMetadata())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "archive-type", JvmStartupOptimizerArchiveType.SCC.name(),
                        "artifact-kind", JvmStartupOptimizerArchiveKind.DIRECTORY.name()));
    }
}
