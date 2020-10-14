package io.quarkus.devtools.commands;

import static io.quarkus.devtools.ProjectTestUtil.checkContains;
import static io.quarkus.devtools.ProjectTestUtil.checkMatches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
    public void createRESTEasy() throws Exception {
        final File file = new File("target/basic-resteasy");
        final Path projectDir = file.toPath();
        ProjectTestUtil.delete(file);
        assertCreateProject(newCreateProject(projectDir)
                .groupId("io.foo")
                .artifactId("resteasy-app")
                .version("1.0.0-FOO")
                .className("my.project.resteasy.FooResource")
                .resourcePath("/foo")
                .extensions(Collections.singleton("resteasy")));

        assertThat(projectDir.resolve(".gitignore"))
                .exists()
                .satisfies(checkMatches("(?s).*target/\\R.*"));
        assertThat(projectDir.resolve("src/main/java/my/project/resteasy/FooResource.java"))
                .exists()
                .satisfies(checkContains("class FooResource"))
                .satisfies(checkContains("@Path(\"/foo\")"));
        assertThat(projectDir.resolve("pom.xml"))
                .exists()
                .satisfies(checkContains("<groupId>io.foo</groupId>"))
                .satisfies(checkContains("<artifactId>resteasy-app</artifactId>"))
                .satisfies(checkContains("<version>1.0.0-FOO</version>"))
                .satisfies(checkContains("<artifactId>quarkus-resteasy</artifactId>"));

        assertThat(projectDir.resolve("README.md"))
                .exists()
                .satisfies(checkContains("./mvnw"));
    }

    @Test
    public void createSpringWeb() throws Exception {
        final File file = new File("target/create-spring");
        final Path projectDir = file.toPath();
        ProjectTestUtil.delete(file);
        assertCreateProject(newCreateProject(projectDir)
                .groupId("io.bar")
                .artifactId("spring-web-app")
                .version("1.0.0-BAR")
                .className("my.project.spring.BarController")
                .resourcePath("/bar")
                .extensions(Collections.singleton("spring-web")));
        assertThat(projectDir.resolve("pom.xml"))
                .exists()
                .satisfies(checkContains("<groupId>io.bar</groupId>"))
                .satisfies(checkContains("<artifactId>spring-web-app</artifactId>"))
                .satisfies(checkContains("<version>1.0.0-BAR</version>"))
                .satisfies(checkContains("<artifactId>quarkus-spring-web</artifactId>"));

        assertThat(projectDir.resolve("src/main/java/my/project/spring/BarController.java"))
                .exists()
                .satisfies(checkContains("@RestController"))
                .satisfies(checkContains("class BarController"))
                .satisfies(checkContains("@RequestMapping(\"/bar\")"));
    }

    @Test
    public void createRESTEasyAndSpringWeb() throws Exception {
        final File file = new File("target/create-spring-resteasy");
        final Path projectDir = file.toPath();
        ProjectTestUtil.delete(file);
        assertCreateProject(newCreateProject(projectDir)
                .artifactId("spring-web-resteasy-app")
                .className("my.project.spring.BarController")
                .resourcePath("/bar")
                .extensions(new HashSet<>(Arrays.asList("resteasy", "spring-web"))));
        assertThat(projectDir.resolve("pom.xml"))
                .exists()
                .satisfies(checkContains("<artifactId>spring-web-resteasy-app</artifactId>"))
                .satisfies(checkContains("<artifactId>quarkus-spring-web</artifactId>"))
                .satisfies(checkContains("<artifactId>quarkus-resteasy</artifactId>"));

        assertThat(projectDir.resolve("src/main/java/org/acme/spring/web/ExampleController.java"))
                .exists()
                .satisfies(checkContains("@RestController"))
                .satisfies(checkContains("class ExampleController"))
                .satisfies(checkContains("@RequestMapping(\"/springweb/hello\")"));

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java"))
                .exists()
                .satisfies(checkContains("class ExampleResource"))
                .satisfies(checkContains("@Path(\"/resteasy/hello\")"));
    }

    @Test
    public void createGradle() throws Exception {
        final File file = new File("target/create-resteasy-gradle");
        final Path projectDir = file.toPath();
        ProjectTestUtil.delete(file);
        assertCreateProject(newCreateProject(projectDir)
                .buildTool(BuildTool.GRADLE)
                .groupId("io.foo")
                .artifactId("resteasy-app")
                .version("1.0.0-FOO")
                .className("my.project.FooResource")
                .resourcePath("/foo")
                .extensions(Collections.singleton("resteasy")));

        assertThat(projectDir.resolve(".gitignore"))
                .exists()
                .satisfies(checkMatches("(?s).*build/\\R.*"))
                .satisfies(checkMatches("(?s).*\\.gradle/\\R.*"));
        assertThat(projectDir.resolve("src/main/java/my/project/FooResource.java"))
                .exists()
                .satisfies(checkContains("@Path(\"/foo\")"));
        assertThat(projectDir.resolve("build.gradle"))
                .exists()
                .satisfies(checkContains("group 'io.foo'"))
                .satisfies(checkContains("version '1.0.0-FOO'"))
                .satisfies(checkContains("implementation 'io.quarkus:quarkus-resteasy'"));

        assertThat(projectDir.resolve("settings.gradle"))
                .exists()
                .satisfies(checkContains("rootProject.name='resteasy-app'"));

        assertThat(projectDir.resolve("README.md"))
                .exists()
                .satisfies(checkContains("./gradlew"));
    }

    private CreateProject newCreateProject(Path dir) {
        return new CreateProject(dir, getPlatformDescriptor());
    }

    @Test
    public void createOnTopOfExisting() throws Exception {
        final File testDir = new File("target/create-existing");
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

    private void assertCreateProject(CreateProject createProject)
            throws QuarkusCommandException {
        final QuarkusCommandOutcome result = createProject
                .quarkusMavenPluginVersion("2.3.5")
                .quarkusGradlePluginVersion("2.3.5-gradle")
                .execute();
        assertTrue(result.isSuccess());
    }

}
