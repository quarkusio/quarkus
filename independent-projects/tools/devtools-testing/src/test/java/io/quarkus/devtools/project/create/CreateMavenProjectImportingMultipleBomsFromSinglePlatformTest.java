package io.quarkus.devtools.project.create;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistriesConfigLocator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CreateMavenProjectImportingMultipleBomsFromSinglePlatformTest {

    private static final String PLATFORM_GROUP_ID_POM_PROP = "quarkus.platform.group-id";
    private static final String PLATFORM_GROUP_ID_POM_EXPR = "${" + PLATFORM_GROUP_ID_POM_PROP + "}";

    private static final String PLATFORM_ARTIFACT_ID_POM_PROP = "quarkus.platform.artifact-id";
    private static final String PLATFORM_ARTIFACT_ID_POM_EXPR = "${" + PLATFORM_ARTIFACT_ID_POM_PROP + "}";

    private static final String PLATFORM_VERSION_POM_PROP = "quarkus.platform.version";
    private static final String PLATFORM_VERSION_POM_EXPR = "${" + PLATFORM_VERSION_POM_PROP + "}";

    private static final String PLATFORM_KEY = "org.acme.platform";
    private static final String PLATFORM_VERSION = "1.0.0";

    @BeforeAll
    public static void setup() throws Exception {
        final Path configDir = getProjectDir("registry-client");
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir)
                // registry
                .newRegistry("registry.acme.org")
                // platform key
                .newPlatform(PLATFORM_KEY)
                // stream id
                .newStream("1.0")
                // release version
                .newRelease("1.0.0")
                .quarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                // foo platform member
                .newMember("acme-foo-bom")
                .addExtension("acme-foo")
                .release()
                // bar platform member
                .newMember("acme-bar-bom")
                .addExtension("acme-bar")
                .release()
                .registry()
                .clientBuilder()
                .build();

        System.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, configDir.resolve("config.yaml").toString());
        System.setProperty("quarkusRegistryClient", "true");
        QuarkusProjectHelper.resetToolsConfig();
    }

    @AfterAll
    public static void cleanup() throws Exception {
        System.clearProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY);
        System.clearProperty("quarkusRegistryClient");
    }

    @Test
    public void testAddExtensionFromNonDefaultBom() throws Exception {
        final Path projectDir = prepareProjectDir("extension-from-non-default-bom");
        final QuarkusProject project = getQuarkusProject(projectDir);
        new CreateProject(project)
                .groupId("org.acme")
                .artifactId("acme-app")
                .version("0.0.1-SNAPSHOT")
                .extensions(new LinkedHashSet<>(Arrays.asList("acme-bar", "acme-foo")))
                .execute();

        assertSinglePlatformModel(projectDir, Arrays.asList("acme-foo-bom", "acme-bar-bom"),
                Arrays.asList("acme-foo", "acme-bar"));
    }

    private void assertSinglePlatformModel(final Path projectDir, final List<String> extraBoms,
            final List<String> extraExtensions)
            throws IOException {

        final List<ArtifactCoords> expectedBoms = new ArrayList<>(extraBoms.size() + 1);
        expectedBoms.add(new ArtifactCoords(PLATFORM_GROUP_ID_POM_EXPR, PLATFORM_ARTIFACT_ID_POM_EXPR, null, "pom",
                PLATFORM_VERSION_POM_EXPR));
        for (String artifactId : extraBoms) {
            expectedBoms
                    .add(new ArtifactCoords(PLATFORM_GROUP_ID_POM_EXPR, artifactId, null, "pom", PLATFORM_VERSION_POM_EXPR));
        }

        final List<ArtifactCoords> expectedExtensions = new ArrayList<>(extraExtensions.size());
        for (String artifactId : extraExtensions) {
            expectedExtensions.add(new ArtifactCoords(PLATFORM_KEY, artifactId, null, "jar", null));
        }

        assertModel(projectDir, expectedBoms, expectedExtensions);
    }

    private void assertModel(final Path projectDir, final List<ArtifactCoords> expectedBoms,
            final List<ArtifactCoords> expectedExtensions) throws IOException {
        final Model model = ModelUtils.readModel(projectDir.resolve("pom.xml"));
        assertThat(model.getProperties().getProperty(PLATFORM_GROUP_ID_POM_PROP)).isEqualTo(PLATFORM_KEY);
        assertThat(model.getProperties().getProperty(PLATFORM_ARTIFACT_ID_POM_PROP)).isEqualTo("quarkus-bom");
        assertThat(model.getProperties().getProperty(PLATFORM_VERSION_POM_PROP)).isEqualTo(PLATFORM_VERSION);

        // TODO the order should be predictable
        assertThat(model.getDependencyManagement().getDependencies().stream()
                .map(d -> new ArtifactCoords(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(),
                        d.getVersion()))
                .collect(Collectors.toList())).containsAll(expectedBoms);

        // TODO the order should be predictable
        assertThat(model.getDependencies().stream()
                .map(d -> new ArtifactCoords(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion()))
                .collect(Collectors.toSet())).containsAll(expectedExtensions);
    }

    private QuarkusProject getQuarkusProject(Path projectDir) {
        return QuarkusProjectHelper.getProject(projectDir, BuildTool.MAVEN);
    }

    private static Path prepareProjectDir(String name) {
        Path projectDir = getProjectDir(name);
        if (Files.exists(projectDir)) {
            IoUtils.recursiveDelete(projectDir);
        }
        try {
            Files.createDirectories(projectDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory " + projectDir, e);
        }
        return projectDir;
    }

    private static Path getProjectDir(String name) {
        return Paths.get("target").resolve("generated-test-projects").resolve(name);
    }
}
