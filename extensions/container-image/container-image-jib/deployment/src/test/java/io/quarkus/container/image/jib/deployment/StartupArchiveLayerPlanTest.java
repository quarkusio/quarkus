package io.quarkus.container.image.jib.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;

class StartupArchiveLayerPlanTest {

    @TempDir
    Path tempDir;

    @Test
    void plansAotFileAtExactContainerPath() throws IOException {
        Path archive = Files.writeString(tempDir.resolve("app.aot"), "cache");

        var plan = StartupArchiveLayerPlan.from(archive, "/work", JvmStartupOptimizerArchiveType.AOT);

        assertThat(plan.archive()).isEqualTo(archive);
        assertThat(plan.destinationDirectory().toString()).isEqualTo("/work");
        assertThat(plan.containerArchive().toString()).isEqualTo("/work/app.aot");
        assertThat(plan.runtimeOption()).isEqualTo("-XX:AOTCache=/work/app.aot");
    }

    @Test
    void plansSccDirectoryAndReadonlyRuntimeOption() throws IOException {
        Path archive = Files.createDirectory(tempDir.resolve("app-scc"));

        var plan = StartupArchiveLayerPlan.from(archive, "/work", JvmStartupOptimizerArchiveType.SCC);

        assertThat(plan.containerArchive().toString()).isEqualTo("/work/app-scc");
        assertThat(plan.runtimeOption())
                .isEqualTo("-Xshareclasses:name=quarkus-app,cacheDir=/work/app-scc,readonly");
    }

    @Test
    void recognizesExistingRuntimeOptions() {
        assertThat(StartupArchiveLayerPlan.containsRuntimeOption(
                List.of("java", "-Xshareclasses:name=other,cacheDir=/cache"), JvmStartupOptimizerArchiveType.SCC))
                .isTrue();
        assertThat(StartupArchiveLayerPlan.containsRuntimeOption(
                List.of("java", "-XX:AOTCache=/cache/app.aot"), JvmStartupOptimizerArchiveType.AOT))
                .isTrue();
        assertThat(StartupArchiveLayerPlan.containsRuntimeOption(
                List.of("java", "-Xmx512m"), JvmStartupOptimizerArchiveType.SCC))
                .isFalse();
    }

    @Test
    void rejectsArchiveWithWrongShape() throws IOException {
        Path directory = Files.createDirectory(tempDir.resolve("app.aot"));
        Path file = Files.writeString(tempDir.resolve("app-scc"), "cache");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> StartupArchiveLayerPlan.from(directory, "/work",
                        JvmStartupOptimizerArchiveType.AOT))
                .withMessageContaining("is not a file");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> StartupArchiveLayerPlan.from(file, "/work",
                        JvmStartupOptimizerArchiveType.SCC))
                .withMessageContaining("is not a directory");
    }

    @Test
    void rejectsContainerPathsWithWhitespace() throws IOException {
        Path archive = Files.writeString(tempDir.resolve("app.aot"), "cache");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> StartupArchiveLayerPlan.from(archive, "/work directory",
                        JvmStartupOptimizerArchiveType.AOT))
                .withMessageContaining("must not contain whitespace");
    }

    @Test
    void appendsStartupArchiveOptionToInheritedJavaToolOptions() {
        assertThat(JibProcessor.appendJavaToolOptions("-Xmx256m", "-XX:AOTCache=/work/app.aot"))
                .isEqualTo("-Xmx256m -XX:AOTCache=/work/app.aot");
        assertThat(JibProcessor.appendJavaToolOptions("", "-XX:AOTCache=/work/app.aot"))
                .isEqualTo("-XX:AOTCache=/work/app.aot");
    }
}
