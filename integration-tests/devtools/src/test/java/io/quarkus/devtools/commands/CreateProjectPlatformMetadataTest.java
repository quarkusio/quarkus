package io.quarkus.devtools.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.SnapshotTesting;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public class CreateProjectPlatformMetadataTest extends PlatformAwareTestBase {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void create(boolean legacyCodegen) throws Exception {
        final File file = new File("target/meta-rest");
        SnapshotTesting.deleteTestDirectory(file);
        createProject(BuildTool.MAVEN, file, "io.quarkus", "basic-rest", "1.0.0-SNAPSHOT", legacyCodegen);
        assertThat(file.toPath().resolve("pom.xml"))
                .exists()
                .satisfies(checkContains("<id>redhat</id>"))
                .satisfies(checkContains("<url>https://maven.repository.redhat.com</url>"))
                .satisfies(checkContains("<snapshots>"))
                .satisfies(checkContains("<releases>"))
                .satisfies(checkContains("<repositories>"))
                .satisfies(checkContains("<pluginRepositories>"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void createGradle(boolean legacyCodegen) throws Exception {
        final File file = new File("target/meta-rest-gradle");
        SnapshotTesting.deleteTestDirectory(file);
        createProject(BuildTool.GRADLE, file, "io.quarkus", "basic-rest", "1.0.0-SNAPSHOT", legacyCodegen);
        assertThat(file.toPath().resolve("build.gradle"))
                .exists()
                .satisfies(checkContains("maven { url \"https://maven.repository.redhat.com\" }"));
    }

    @Test
    public void createGradleKotlin() throws Exception {
        final File file = new File("target/meta-rest-gradle-kts");
        SnapshotTesting.deleteTestDirectory(file);
        createProject(BuildTool.GRADLE_KOTLIN_DSL, file, "io.quarkus", "basic-rest", "1.0.0-SNAPSHOT", false);
        assertThat(file.toPath().resolve("build.gradle.kts"))
                .exists()
                .satisfies(checkContains("maven { url = uri(\"https://maven.repository.redhat.com\") }"));
    }

    private Consumer<Path> checkContains(String s) {
        return (p) -> assertThat(Files.contentOf(p.toFile(), StandardCharsets.UTF_8)).contains(s);
    }

    private Map<String, Object> getMetadata() throws java.io.IOException {
        return JSON_MAPPER.reader().readValue(CreateProjectPlatformMetadataTest.class.getResource("/platform-metadata.json"),
                Map.class);
    }

    private void createProject(BuildTool buildTool, File file, String groupId, String artifactId, String version,
            boolean legacyCodegen)
            throws QuarkusCommandException, IOException {
        final QuarkusPlatformDescriptor platformDescriptor = getPlatformDescriptor();
        final QuarkusPlatformDescriptor spy = spy(platformDescriptor);
        when(spy.getMetadata()).thenReturn(getMetadata());
        final QuarkusCommandOutcome result = new CreateProject(file.toPath(), spy)
                .buildTool(buildTool)
                .groupId(groupId)
                .artifactId(artifactId)
                .legacyCodegen(legacyCodegen)
                .version(version)
                .quarkusMavenPluginVersion("2.3.5")
                .quarkusGradlePluginVersion("2.3.5-gradle")
                .execute();
        assertTrue(result.isSuccess());
    }
}
