package io.quarkus.gradle.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.buildinit.specs.BuildInitConfig;
import org.gradle.buildinit.specs.BuildInitParameter;
import org.gradle.buildinit.specs.BuildInitSpec;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.devtools.testing.RegistryClientTest;

class QuarkusAppInitGeneratorTest {

    @RegisterExtension
    static final RegistryClientTest registryClientTest = new RegistryClientTest();

    @TempDir
    Path workDir;

    @Test
    void generatesAQuarkusGradleKotlinDslProject() throws IOException {
        Path targetDir = generate(configFor("acme-app").groupId("com.example"));

        assertThat(targetDir.resolve("build.gradle.kts")).exists();
        assertThat(targetDir.resolve("src/main/java")).isDirectory();
        assertThat(Files.readString(targetDir.resolve("settings.gradle.kts")))
                .contains("rootProject.name=\"acme-app\"");
        assertThat(Files.readString(targetDir.resolve("build.gradle.kts")))
                .contains("group = \"com.example\"");
    }

    @Test
    void defaultsGroupIdWhenNotProvided() throws IOException {
        Path targetDir = generate(configFor("acme-app"));

        assertThat(Files.readString(targetDir.resolve("build.gradle.kts")))
                .contains("group = \"org.acme\"");
    }

    @Test
    void addsRequestedExtensions() throws IOException {
        Path targetDir = generate(configFor("acme-app").extensions("openapi"));

        assertThat(Files.readString(targetDir.resolve("build.gradle.kts")))
                .contains("quarkus-smallrye-openapi");
    }

    @Test
    void generatesGroovyDslProjectWhenRequested() throws IOException {
        Path targetDir = generate(configFor("acme-app").dsl("groovy"));

        assertThat(targetDir.resolve("build.gradle")).exists();
        assertThat(targetDir.resolve("settings.gradle")).exists();
        assertThat(targetDir.resolve("build.gradle.kts")).doesNotExist();
    }

    @Test
    void defaultsToKotlinDslWhenNotRequested() throws IOException {
        Path targetDir = generate(configFor("acme-app"));

        assertThat(targetDir.resolve("build.gradle.kts")).exists();
    }

    @Test
    void failsWithGradleExceptionWhenTargetDirectoryIsNotEmpty() throws IOException {
        Directory generatedProjectDir = generatedProjectDir();
        Path targetDir = workDir.resolve("generated-project");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("pre-existing.txt"), "leftover file");

        assertThatThrownBy(
                () -> new QuarkusAppInitGenerator().generate(configFor("acme-app").build(), generatedProjectDir))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("Failed to generate the Quarkus project");
    }

    @Test
    void appliesCustomVersionWhenProvided() throws IOException {
        Path targetDir = generate(configFor("acme-app").version("2.0.0"));

        assertThat(Files.readString(targetDir.resolve("build.gradle.kts")))
                .contains("version = \"2.0.0\"");
    }

    @Test
    void defaultsVersionWhenNotProvided() throws IOException {
        Path targetDir = generate(configFor("acme-app"));

        assertThat(Files.readString(targetDir.resolve("build.gradle.kts")))
                .contains("version = \"1.0.0-SNAPSHOT\"");
    }

    @Test
    void appliesDescriptionToReadmeWhenProvided() throws IOException {
        Path targetDir = generate(configFor("acme-app").description("A tiny demo app"));

        assertThat(Files.readString(targetDir.resolve("README.md"))).contains("A tiny demo app");
    }

    @Test
    void customizesRestResourceClassNameAndPath() throws IOException {
        Path targetDir = generate(configFor("acme-app").className("Hello").path("/hello"));

        Path resourceFile = targetDir.resolve("src/main/java/org/acme/Hello.java");
        assertThat(resourceFile).exists();
        assertThat(Files.readString(resourceFile)).contains("/hello");
    }

    @Test
    void skipsDockerfilesWhenRequested() throws IOException {
        Path targetDir = generate(configFor("acme-app").noDockerfiles());

        assertThat(targetDir.resolve("src/main/docker")).doesNotExist();
    }

    @Test
    void generatesDockerfilesByDefault() throws IOException {
        Path targetDir = generate(configFor("acme-app"));

        assertThat(targetDir.resolve("src/main/docker")).isDirectory();
    }

    @Test
    void skipsBuildToolWrapperWhenRequested() throws IOException {
        Path targetDir = generate(configFor("acme-app").noBuildToolWrapper());

        assertThat(targetDir.resolve("gradlew")).doesNotExist();
    }

    @Test
    void skipsCodeGenerationWhenRequested() throws IOException {
        Path targetDir = generate(configFor("acme-app").noCode());

        try (var javaFiles = Files.walk(targetDir.resolve("src"))) {
            assertThat(javaFiles.filter(path -> path.toString().endsWith(".java"))).isEmpty();
        }
        assertThat(targetDir.resolve("build.gradle.kts")).exists();
    }

    @Test
    void selectsKotlinSourceTypeWhenKotlinExtensionRequested() throws IOException {
        Path targetDir = generate(configFor("acme-app").extensions("quarkus-kotlin"));

        assertThat(targetDir.resolve("src/main/kotlin")).isDirectory();
        assertThat(Files.readString(targetDir.resolve("build.gradle.kts"))).contains("kotlin(\"jvm\")");
    }

    @Test
    void acceptsAnExplicitQuarkusVersion() throws IOException {
        Path targetDir = generate(configFor("acme-app").quarkusVersion(System.getProperty("project.version")));

        assertThat(Files.readString(targetDir.resolve("gradle.properties")))
                .contains("quarkusPlatformVersion=" + System.getProperty("project.version"));
    }

    private Path generate(ConfigBuilder config) throws IOException {
        Directory generatedProjectDir = generatedProjectDir();
        new QuarkusAppInitGenerator().generate(config.build(), generatedProjectDir);
        return workDir.resolve("generated-project");
    }

    /**
     * Targets a not-yet-created subdirectory of the {@link ProjectBuilder} project directory, since
     * {@link ProjectBuilder} writes its own state into whatever directory it's given, which would otherwise trip
     * the codestart engine's "target directory must be empty" check.
     */
    private Directory generatedProjectDir() {
        return ProjectBuilder.builder().withProjectDir(workDir.toFile()).build()
                .getLayout().getProjectDirectory().dir("generated-project");
    }

    private ConfigBuilder configFor(String projectName) {
        return new ConfigBuilder(projectName);
    }

    private static final class ConfigBuilder {

        private final Map<BuildInitParameter<?>, Object> arguments = new HashMap<>();

        ConfigBuilder(String projectName) {
            arguments.put(ProjectNameParameter.INSTANCE, projectName);
        }

        ConfigBuilder groupId(String groupId) {
            arguments.put(GroupIdParameter.INSTANCE, groupId);
            return this;
        }

        ConfigBuilder extensions(String extensions) {
            arguments.put(ExtensionsParameter.INSTANCE, extensions);
            return this;
        }

        ConfigBuilder dsl(String dsl) {
            arguments.put(DslParameter.INSTANCE, dsl);
            return this;
        }

        ConfigBuilder version(String version) {
            arguments.put(StringInitParameter.VERSION, version);
            return this;
        }

        ConfigBuilder description(String description) {
            arguments.put(StringInitParameter.DESCRIPTION, description);
            return this;
        }

        ConfigBuilder className(String className) {
            arguments.put(StringInitParameter.CLASS_NAME, className);
            return this;
        }

        ConfigBuilder path(String path) {
            arguments.put(StringInitParameter.PATH, path);
            return this;
        }

        ConfigBuilder quarkusVersion(String quarkusVersion) {
            arguments.put(StringInitParameter.QUARKUS_VERSION, quarkusVersion);
            return this;
        }

        ConfigBuilder noDockerfiles() {
            arguments.put(BooleanInitParameter.NO_DOCKERFILES, Boolean.TRUE);
            return this;
        }

        ConfigBuilder noBuildToolWrapper() {
            arguments.put(BooleanInitParameter.NO_BUILD_TOOL_WRAPPER, Boolean.TRUE);
            return this;
        }

        ConfigBuilder noCode() {
            arguments.put(BooleanInitParameter.NO_CODE, Boolean.TRUE);
            return this;
        }

        BuildInitConfig build() {
            return new BuildInitConfig() {
                @Override
                public BuildInitSpec getBuildSpec() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Map<BuildInitParameter<?>, Object> getArguments() {
                    return arguments;
                }
            };
        }
    }
}
