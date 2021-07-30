package io.quarkus.devtools.project.create;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.junit.jupiter.api.AfterAll;

public abstract class MultiplePlatformBomsTestBase {

    private static final String PLATFORM_GROUP_ID_POM_PROP = "quarkus.platform.group-id";
    private static final String PLATFORM_GROUP_ID_POM_EXPR = "${" + PLATFORM_GROUP_ID_POM_PROP + "}";

    private static final String PLATFORM_ARTIFACT_ID_POM_PROP = "quarkus.platform.artifact-id";
    private static final String PLATFORM_ARTIFACT_ID_POM_EXPR = "${" + PLATFORM_ARTIFACT_ID_POM_PROP + "}";

    private static final String PLATFORM_VERSION_POM_PROP = "quarkus.platform.version";
    private static final String PLATFORM_VERSION_POM_EXPR = "${" + PLATFORM_VERSION_POM_PROP + "}";

    private static Path configDir;
    private static String prevConfigPath;
    private static String prevRegistryClient;

    static Path configDir() {
        return configDir == null ? configDir = getProjectDir("registry-client") : configDir;
    }

    static void enableRegistryClient() {
        prevConfigPath = System.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY,
                configDir.resolve("config.yaml").toString());
        prevRegistryClient = System.setProperty("quarkusRegistryClient", "true");
        QuarkusProjectHelper.reset();
    }

    @AfterAll
    public static void cleanup() throws Exception {
        resetProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, prevConfigPath);
        resetProperty("quarkusRegistryClient", prevRegistryClient);
    }

    private static void resetProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    protected abstract String getMainPlatformKey();

    protected QuarkusCommandOutcome addExtensions(Path projectDir, List<String> extensions)
            throws IOException, QuarkusCommandException {

        final Path pomXml = projectDir.resolve("pom.xml");
        final Model model = ModelUtils.readModel(pomXml);
        if (model.getRepositories().isEmpty()) {
            final Repository r = new Repository();
            r.setId("devtools-registry-client-test-repo");
            r.setUrl(TestRegistryClientBuilder.getMavenRepoDir(configDir).toAbsolutePath().toUri().toURL().toString());
            model.getRepositories().add(r);
            ModelUtils.persistModel(pomXml, model);
        }

        return new AddExtensions(getQuarkusProject(projectDir))
                .extensions(new HashSet<>(extensions))
                .execute();
    }

    protected QuarkusCommandOutcome createProject(Path projectDir, List<String> extensions)
            throws Exception {
        return createProject(projectDir, null, extensions);
    }

    protected QuarkusCommandOutcome createProject(Path projectDir, String quarkusVersion, List<String> extensions)
            throws Exception {
        return new CreateProject(
                quarkusVersion == null ? getQuarkusProject(projectDir) : getQuarkusProject(projectDir, quarkusVersion))
                        .groupId("org.acme")
                        .artifactId("acme-app")
                        .version("0.0.1-SNAPSHOT")
                        .extensions(new HashSet<>(extensions))
                        .execute();
    }

    protected List<ArtifactCoords> toPlatformExtensionCoords(String... artifactIds) {
        return toPlatformExtensionCoords(Arrays.asList(artifactIds));
    }

    protected List<ArtifactCoords> toPlatformExtensionCoords(final List<String> extraExtensions) {
        final List<ArtifactCoords> expectedExtensions = new ArrayList<>(extraExtensions.size());
        for (String artifactId : extraExtensions) {
            expectedExtensions.add(platformExtensionCoords(artifactId));
        }
        return expectedExtensions;
    }

    protected List<ArtifactCoords> toPlatformBomCoords(String... artifactIds) {
        return toPlatformBomCoords(Arrays.asList(artifactIds));
    }

    protected List<ArtifactCoords> toPlatformBomCoords(final List<String> extraBoms) {
        final List<ArtifactCoords> expectedBoms = new ArrayList<>(extraBoms.size() + 1);
        expectedBoms.add(mainPlatformBom());
        for (String artifactId : extraBoms) {
            expectedBoms.add(platformMemberBomCoords(artifactId));
        }
        return expectedBoms;
    }

    protected ArtifactCoords mainPlatformBom() {
        return new ArtifactCoords(PLATFORM_GROUP_ID_POM_EXPR, PLATFORM_ARTIFACT_ID_POM_EXPR, "pom",
                PLATFORM_VERSION_POM_EXPR);
    }

    protected void assertModel(final Path projectDir, final List<ArtifactCoords> expectedBoms,
            final List<ArtifactCoords> expectedExtensions, String platformVersion) throws IOException {
        final Model model = ModelUtils.readModel(projectDir.resolve("pom.xml"));
        assertThat(model.getProperties().getProperty(PLATFORM_GROUP_ID_POM_PROP)).isEqualTo(getMainPlatformKey());
        assertThat(model.getProperties().getProperty(PLATFORM_ARTIFACT_ID_POM_PROP)).isEqualTo("quarkus-bom");
        assertThat(model.getProperties().getProperty(PLATFORM_VERSION_POM_PROP)).isEqualTo(platformVersion);

        final List<ArtifactCoords> actualBoms = model.getDependencyManagement().getDependencies().stream()
                .map(d -> new ArtifactCoords(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(),
                        d.getVersion()))
                .collect(Collectors.toList());
        // TODO the order should be predictable
        assertThat(actualBoms).containsAll(expectedBoms);
        assertThat(expectedBoms).containsAll(actualBoms);
        //assertThat(model.getDependencyManagement().getDependencies().size()).isEqualTo(expectedBoms.size());

        // TODO the order should be predictable
        assertThat(model.getDependencies().stream()
                .map(d -> new ArtifactCoords(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion()))
                .collect(Collectors.toSet())).containsAll(expectedExtensions);
    }

    ArtifactCoords platformExtensionCoords(String artifactId) {
        return new ArtifactCoords(getMainPlatformKey(), artifactId, "jar", null);
    }

    static ArtifactCoords platformMemberBomCoords(String artifactId) {
        return new ArtifactCoords(PLATFORM_GROUP_ID_POM_EXPR, artifactId, "pom", PLATFORM_VERSION_POM_EXPR);
    }

    protected QuarkusProject getQuarkusProject(Path projectDir) {
        return QuarkusProjectHelper.getProject(projectDir, BuildTool.MAVEN);
    }

    protected QuarkusProject getQuarkusProject(Path projectDir, String quarkusVersion) {
        return QuarkusProjectHelper.getProject(projectDir, BuildTool.MAVEN, quarkusVersion);
    }

    static Path newProjectDir(String name) {
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

    static Path getProjectDir(String name) {
        return Paths.get("target").resolve("generated-test-projects").resolve(name);
    }
}
