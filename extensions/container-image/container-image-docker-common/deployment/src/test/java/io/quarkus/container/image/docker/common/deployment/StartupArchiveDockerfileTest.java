package io.quarkus.container.image.docker.common.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;

class StartupArchiveDockerfileTest {

    @TempDir
    Path tempDir;

    @Test
    void plansAotFileFromItsParentBuildContext() throws IOException {
        Path archive = Files.writeString(tempDir.resolve("app.aot"), "cache");

        Path context;
        try (var plan = StartupArchiveDockerfile.prepare(tempDir.resolve("staging"), "example/base:latest", archive,
                "/work", JvmStartupOptimizerArchiveType.AOT)) {
            context = plan.contextDirectory();
            assertThat(context).isNotEqualTo(tempDir);
            assertThat(context.resolve("app.aot")).hasContent("cache");
            assertThat(plan.containerArchive()).isEqualTo("/work/app.aot");
            assertThat(plan.dockerfile())
                    .contains("COPY [\"app.aot\", \"/work/app.aot\"]")
                    .contains(
                            "ENV JAVA_TOOL_OPTIONS=\"${JAVA_TOOL_OPTIONS} -XX:AOTCache=/work/app.aot\"");
        }
        assertThat(context).doesNotExist();
    }

    @Test
    void preservesSccDirectoryAtItsExactContainerPath() throws IOException {
        Path archive = Files.createDirectory(tempDir.resolve("app-scc"));
        Files.writeString(archive.resolve("cache"), "cache");

        try (var plan = StartupArchiveDockerfile.prepare(tempDir.resolve("staging"), "example/base:latest", archive,
                "/work/", JvmStartupOptimizerArchiveType.SCC)) {
            assertThat(plan.contextDirectory().resolve("app-scc/cache")).hasContent("cache");
            assertThat(plan.containerArchive()).isEqualTo("/work/app-scc");
            assertThat(plan.dockerfile())
                    .contains("COPY [\"app-scc\", \"/work/app-scc/\"]")
                    .contains(
                            "ENV JAVA_TOOL_OPTIONS=\"${JAVA_TOOL_OPTIONS} -Xshareclasses:name=quarkus-app,cacheDir=/work/app-scc,readonly\"");
        }
    }

    @Test
    void rejectsArchiveWithWrongShape() throws IOException {
        Path directory = Files.createDirectory(tempDir.resolve("app.aot"));
        Path file = Files.writeString(tempDir.resolve("app-scc"), "cache");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> StartupArchiveDockerfile.prepare(tempDir.resolve("staging"), "base", directory,
                        "/work", JvmStartupOptimizerArchiveType.AOT))
                .withMessageContaining("is not a file");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> StartupArchiveDockerfile.prepare(tempDir.resolve("staging"), "base", file,
                        "/work", JvmStartupOptimizerArchiveType.SCC))
                .withMessageContaining("is not a directory");
    }

    @Test
    void rejectsContainerPathsWithWhitespaceBeforeCreatingAContext() throws IOException {
        Path archive = Files.writeString(tempDir.resolve("app.aot"), "cache");
        Path staging = tempDir.resolve("staging");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> StartupArchiveDockerfile.prepare(staging, "base", archive,
                        "/work directory", JvmStartupOptimizerArchiveType.AOT))
                .withMessageContaining("must not contain whitespace");
        assertThat(staging).doesNotExist();
    }
}
