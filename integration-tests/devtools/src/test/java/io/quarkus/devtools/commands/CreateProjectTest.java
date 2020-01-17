package io.quarkus.devtools.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.contentOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.codegen.writer.FileProjectWriter;
import io.quarkus.maven.utilities.MojoUtils;

public class CreateProjectTest extends PlatformAwareTestBase {
    @Test
    public void create() throws Exception {
        final File file = new File("target/basic-rest");
        ProjectTestUtil.delete(file);
        createProject(file, "io.quarkus", "basic-rest", "1.0.0-SNAPSHOT");

        final File gitignore = new File(file, ".gitignore");
        assertTrue(gitignore.exists());
        final String gitignoreContent = new String(Files.readAllBytes(gitignore.toPath()), StandardCharsets.UTF_8);
        assertThat(gitignoreContent).matches("(?s).*target/\\R.*");
    }

    @Test
    public void createGradle() throws Exception {
        final File file = new File("target/basic-rest-gradle");
        ProjectTestUtil.delete(file);
        createProject(BuildTool.GRADLE, file, "io.quarkus", "basic-rest", "1.0.0-SNAPSHOT");

        final File gitignore = new File(file, ".gitignore");
        assertTrue(gitignore.exists());
        final String gitignoreContent = new String(Files.readAllBytes(gitignore.toPath()), StandardCharsets.UTF_8);
        assertThat(gitignoreContent).doesNotMatch("(?s).*target/\\R.*");
        assertThat(gitignoreContent).matches("(?s).*build/\\R.*");
        assertThat(gitignoreContent).matches("(?s).*\\.gradle/\\R.*");

        assertThat(new File(file, "README.md")).exists();
        assertThat(contentOf(new File(file, "README.md"), "UTF-8")).contains("./gradlew");
    }

    @Test
    public void createOnTopOfExisting() throws Exception {
        final File testDir = new File("target/existing");
        ProjectTestUtil.delete(testDir);
        testDir.mkdirs();

        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("org.acme");
        model.setArtifactId("foobar");
        model.setVersion("10.1.2");
        final File pom = new File(testDir, "pom.xml");
        MojoUtils.write(model, pom);
        assertThatExceptionOfType(QuarkusCommandException.class).isThrownBy(() -> {
            new CreateProject(testDir.toPath(), getPlatformDescriptor())
                    .groupId("something.is")
                    .artifactId("wrong")
                    .version("1.0.0-SNAPSHOT")
                    .className("org.foo.MyResource")
                    .execute();
        }).withRootCauseInstanceOf(IOException.class);
    }

    @Test
    @Timeout(2)
    @DisplayName("Should create correctly multiple times in parallel with multiple threads")
    void createMultipleTimes() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        final CountDownLatch latch = new CountDownLatch(20);

        List<Callable<Void>> collect = IntStream.range(0, 20).boxed().map(i -> (Callable<Void>) () -> {
            File tempDir = Files.createTempDirectory("test").toFile();
            FileProjectWriter write = new FileProjectWriter(tempDir);
            final QuarkusCommandOutcome result = new CreateProject(tempDir.toPath(), getPlatformDescriptor())
                    .groupId("org.acme")
                    .artifactId("acme")
                    .version("1.0.0-SNAPSHOT")
                    .className("org.acme.MyResource")
                    .execute();
            assertTrue(result.isSuccess());
            latch.countDown();
            write.close();
            tempDir.delete();
            return null;
        }).collect(Collectors.toList());
        executorService.invokeAll(collect);
        latch.await();
    }

    private void createProject(final File file, String groupId, String artifactId, String version)
            throws QuarkusCommandException {
        createProject(BuildTool.MAVEN, file, groupId, artifactId, version);
    }

    private void createProject(BuildTool buildTool, File file, String groupId, String artifactId, String version)
            throws QuarkusCommandException {
        final QuarkusCommandOutcome result = new CreateProject(file.toPath(), getPlatformDescriptor())
                .buildTool(buildTool)
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .quarkusMavenPluginVersion("2.3.5")
                .quarkusGradlePluginVersion("2.3.5-gradle")
                .execute();
        assertTrue(result.isSuccess());
    }
}
