package io.quarkus.devtools.commands;

import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;
import static io.quarkus.devtools.testing.SnapshotTesting.checkMatches;
import static io.quarkus.platform.tools.ToolsConstants.PROP_COMPILER_PLUGIN_VERSION;
import static io.quarkus.platform.tools.ToolsConstants.PROP_SUREFIRE_PLUGIN_VERSION;
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
import java.util.Properties;
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

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.codegen.writer.FileProjectWriter;
import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.devtools.testing.SnapshotTesting;
import io.quarkus.maven.utilities.MojoUtils;

public class CreateProjectTest extends PlatformAwareTestBase {
    @Test
    public void createRESTEasy() throws Exception {
        final File file = new File("target/basic-resteasy");
        final Path projectDir = file.toPath();
        SnapshotTesting.deleteTestDirectory(file);
        assertCreateProject(newCreateProject(projectDir)
                .groupId("org.acme.foo")
                .artifactId("resteasy-app")
                .version("1.0.0-FOO")
                .className("org.acme.getting.started.GreetingResource")
                .resourcePath("/foo")
                .extensions(Collections.singleton("resteasy")));
        final Properties quarkusProp = getQuarkusProperties();
        assertThat(projectDir.resolve(".gitignore"))
                .exists()
                .satisfies(checkMatches("(?s).*target/\\R.*"));
        assertThat(projectDir.resolve("src/main/java/org/acme/getting/started/GreetingResource.java"))
                .exists()
                .satisfies(checkContains("package org.acme.getting.started;"))
                .satisfies(checkContains("class GreetingResource"))
                .satisfies(checkContains("@Path(\"/foo\")"));
        assertThat(projectDir.resolve("pom.xml"))
                .exists()
                .satisfies(checkContains("<groupId>org.acme.foo</groupId>"))
                .satisfies(checkContains("<artifactId>resteasy-app</artifactId>"))
                .satisfies(checkContains("<version>1.0.0-FOO</version>"))
                .satisfies(checkContains(
                        "<surefire-plugin.version>" + quarkusProp.getProperty(PROP_SUREFIRE_PLUGIN_VERSION)
                                + "</surefire-plugin.version>"))
                .satisfies(checkContains(
                        "<compiler-plugin.version>" + quarkusProp.getProperty(PROP_COMPILER_PLUGIN_VERSION)
                                + "</compiler-plugin.version>"))
                .satisfies(checkContains("<artifactId>quarkus-resteasy</artifactId>"));

        assertThat(projectDir.resolve("README.md"))
                .exists()
                .satisfies(checkContains("./mvnw"));
    }

    @Test
    public void createSpringWeb() throws Exception {
        final File file = new File("target/create-spring");
        final Path projectDir = file.toPath();
        SnapshotTesting.deleteTestDirectory(file);
        assertCreateProject(newCreateProject(projectDir)
                .groupId("org.acme.bar")
                .packageName("org.acme.bar.spr")
                .artifactId("spring-web-app")
                .version("1.0.0-BAR")
                .className("BarController")
                .resourcePath("/bar")
                .extensions(Collections.singleton("spring-web")));
        assertThat(projectDir.resolve("pom.xml"))
                .exists()
                .satisfies(checkContains("<groupId>org.acme.bar</groupId>"))
                .satisfies(checkContains("<artifactId>spring-web-app</artifactId>"))
                .satisfies(checkContains("<version>1.0.0-BAR</version>"))
                .satisfies(checkContains("<artifactId>quarkus-spring-web</artifactId>"));

        assertThat(projectDir.resolve("src/main/java/org/acme/bar/spr/BarController.java"))
                .exists()
                .satisfies(checkContains("package org.acme.bar.spr"))
                .satisfies(checkContains("@RestController"))
                .satisfies(checkContains("class BarController"))
                .satisfies(checkContains("@RequestMapping(\"/bar\")"));
    }

    @Test
    public void createRESTEasyAndSpringWeb() throws Exception {
        final File file = new File("target/create-spring-resteasy");
        final Path projectDir = file.toPath();
        SnapshotTesting.deleteTestDirectory(file);
        assertCreateProject(newCreateProject(projectDir)
                .artifactId("spring-web-resteasy-app")
                .className("BarController")
                .packageName("io.test")
                .resourcePath("/bar")
                .extensions(new HashSet<>(Arrays.asList("resteasy", "spring-web"))));
        assertThat(projectDir.resolve("pom.xml"))
                .exists()
                .satisfies(checkContains("<artifactId>spring-web-resteasy-app</artifactId>"))
                .satisfies(checkContains("<artifactId>quarkus-spring-web</artifactId>"))
                .satisfies(checkContains("<artifactId>quarkus-resteasy</artifactId>"));

        assertThat(projectDir.resolve("src/main/java/io/test/GreetingController.java"))
                .exists()
                .satisfies(checkContains("package io.test;"))
                .satisfies(checkContains("@RestController"))
                .satisfies(checkContains("class GreetingController"))
                .satisfies(checkContains("@RequestMapping(\"/greeting\")"));

        assertThat(projectDir.resolve("src/main/java/io/test/GreetingResource.java"))
                .exists()
                .satisfies(checkContains("package io.test;"))
                .satisfies(checkContains("class GreetingResource"))
                .satisfies(checkContains("@Path(\"/hello\")"));
    }

    @Test
    public void createGradle() throws Exception {
        final File file = new File("target/create-resteasy-gradle");
        final Path projectDir = file.toPath();
        SnapshotTesting.deleteTestDirectory(file);
        assertCreateProject(newCreateProject(projectDir, BuildTool.GRADLE)
                .groupId("io.foo")
                .packageName("my.project")
                .artifactId("resteasy-app")
                .version("1.0.0-FOO")
                .className("FooResource")
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
        return newCreateProject(dir, BuildTool.MAVEN);
    }

    private CreateProject newCreateProject(Path dir, BuildTool buildTool) {
        return new CreateProject(QuarkusProjectHelper.getProject(dir, buildTool));
    }

    @Test
    public void createOnTopOfExisting() throws Exception {
        final File testDir = new File("target/create-existing");
        SnapshotTesting.deleteTestDirectory(testDir);
        testDir.mkdirs();

        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("org.acme");
        model.setArtifactId("foobar");
        model.setVersion("10.1.2");
        final File pom = new File(testDir, "pom.xml");
        MojoUtils.write(model, pom);
        assertThatExceptionOfType(QuarkusCommandException.class).isThrownBy(() -> {
            final QuarkusProject project = QuarkusProjectHelper.getProject(testDir.toPath(), BuildTool.MAVEN);
            new CreateProject(project)
                    .groupId("something.is")
                    .artifactId("wrong")
                    .version("1.0.0-SNAPSHOT")
                    .className("org.foo.MyResource")
                    .execute();
        }).withRootCauseInstanceOf(IOException.class);
    }

    @Test
    @Timeout(3)
    @DisplayName("Should create correctly multiple times in parallel with multiple threads")
    void createMultipleTimes() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        final CountDownLatch latch = new CountDownLatch(15);

        List<Callable<Void>> collect = IntStream.range(0, 15).boxed().map(i -> (Callable<Void>) () -> {
            File tempDir = Files.createTempDirectory("test").toFile();
            FileProjectWriter write = new FileProjectWriter(tempDir);
            final QuarkusProject project = QuarkusProjectHelper.getProject(tempDir.toPath(), BuildTool.MAVEN);
            final QuarkusCommandOutcome result = new CreateProject(project)
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
                .quarkusPluginVersion("2.3.5")
                .execute();
        assertTrue(result.isSuccess());
    }

}
