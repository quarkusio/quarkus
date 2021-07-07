package io.quarkus.devtools.project.create;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
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

    private static String prevConfigPath;
    private static String prevRegistryClient;

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
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.4 release
                .newRelease("2.0.4")
                .quarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                // foo platform member
                .newMember("acme-foo-bom").addExtension("acme-foo").release()
                .stream().platform()
                // 1.0 STREAM
                .newStream("1.0")
                // 1.0.1 release
                .newRelease("1.0.1")
                .quarkusVersion("1.1.2")
                .addCoreMember()
                .newMember("acme-foo-bom").addExtension("acme-foo").release()
                .newMember("acme-baz-bom").addExtension("acme-baz").release()
                .stream()
                // 1.0.0 release
                .newRelease("1.0.0")
                .quarkusVersion("1.1.1")
                .addCoreMember()
                .newMember("acme-foo-bom").addExtension("acme-foo").release()
                .newMember("acme-bar-bom").addExtension("acme-bar").release()
                .newMember("acme-baz-bom").addExtension("acme-baz").release()
                .registry()
                // NON-PLATFORM EXTENSION CATALOG FOR QUARKUS 2.2.2
                .newNonPlatformCatalog("2.2.2")
                .addExtension("org.other", "other-extension", "6.0")
                .addExtension("org.other", "other-six-zero", "6.0")
                .registry()
                // NON-PLATFORM EXTENSION CATALOG FOR QUARKUS 1.1.2
                .newNonPlatformCatalog("1.1.2")
                .addExtension("org.other", "other-extension", "5.1")
                .addExtension("org.other", "other-five-one", "5.1")
                .registry()
                // NON-PLATFORM EXTENSION CATALOG FOR QUARKUS 1.1.1
                .newNonPlatformCatalog("1.1.1")
                .addExtension("org.other", "other-extension", "5.0")
                .addExtension("org.other", "other-five-zero", "5.0")
                .registry()
                .clientBuilder()
                .build();

        prevConfigPath = System.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY,
                configDir.resolve("config.yaml").toString());
        prevRegistryClient = System.setProperty("quarkusRegistryClient", "true");
        QuarkusProjectHelper.resetToolsConfig();
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

    @Test
    public void testAddNonPlatformExtensionCompatibleWithTheOldestQuarkusVersion() throws Exception {
        final Path projectDir = newProjectDir("non-platform-extension-oldest-quarkus-version");
        createProject(projectDir, Arrays.asList("acme-foo", "other-extension", "other-five-zero"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:5.0"));
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-five-zero:5.0"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom"), expectedExtensions, "1.0.0");
    }

    @Test
    public void testAddNonPlatformExtensionCompatibleWithTheLatestQuarkusVersion() throws Exception {
        final Path projectDir = newProjectDir("non-platform-extension-latest-quarkus-version");
        createProject(projectDir, Arrays.asList("acme-foo", "other-extension", "other-six-zero"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:6.0"));
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-six-zero:6.0"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom"), expectedExtensions, "2.0.4");
    }

    @Test
    public void testAddNonPlatformExtensionCompatibleWithQuarkusVersion112() throws Exception {
        final Path projectDir = newProjectDir("non-platform-extension-quarkus-version-112");
        createProject(projectDir, Arrays.asList("acme-foo", "other-extension", "other-five-one"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:5.1"));
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-five-one:5.1"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom"), expectedExtensions, "1.0.1");
    }

    @Test
    public void testAddExtensionsPresentInTheLatestReleaseOfTheLatestStream() throws Exception {
        final Path projectDir = newProjectDir("latest-release-latest-stream");
        createProject(projectDir, Arrays.asList("acme-foo", "other-extension"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:6.0"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom"), expectedExtensions, "2.0.4");
    }

    @Test
    public void testAddExtensionsPresentInTheLatestReleaseOfAnOldStream() throws Exception {
        final Path projectDir = newProjectDir("latest-release-old-stream");
        createProject(projectDir, Arrays.asList("acme-baz", "acme-foo", "other-extension"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo", "acme-baz");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:5.1"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom", "acme-baz-bom"), expectedExtensions, "1.0.1");
    }

    @Test
    public void testAddExtensionPresentInTheOldReleaseOfAnOldStream() throws Exception {
        final Path projectDir = newProjectDir("old-release-old-stream");
        createProject(projectDir, Arrays.asList("acme-bar", "acme-foo", "other-extension"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo", "acme-bar");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:5.0"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom", "acme-bar-bom"),
                expectedExtensions, "1.0.0");
    }

    private QuarkusCommandOutcome createProject(Path projectDir, List<String> extensions)
            throws IOException, QuarkusCommandException {
        return new CreateProject(getQuarkusProject(projectDir))
                .groupId("org.acme")
                .artifactId("acme-app")
                .version("0.0.1-SNAPSHOT")
                .extensions(new HashSet<>(extensions))
                .execute();
    }

    private List<ArtifactCoords> toPlatformExtensionCoords(String... artifactIds) {
        return toPlatformExtensionCoords(Arrays.asList(artifactIds));
    }

    private List<ArtifactCoords> toPlatformExtensionCoords(final List<String> extraExtensions) {
        final List<ArtifactCoords> expectedExtensions = new ArrayList<>(extraExtensions.size());
        for (String artifactId : extraExtensions) {
            expectedExtensions.add(platformExtensionCoords(artifactId));
        }
        return expectedExtensions;
    }

    private List<ArtifactCoords> toPlatformBomCoords(String... artifactIds) {
        return toPlatformBomCoords(Arrays.asList(artifactIds));
    }

    private List<ArtifactCoords> toPlatformBomCoords(final List<String> extraBoms) {
        final List<ArtifactCoords> expectedBoms = new ArrayList<>(extraBoms.size() + 1);
        expectedBoms.add(new ArtifactCoords(PLATFORM_GROUP_ID_POM_EXPR, PLATFORM_ARTIFACT_ID_POM_EXPR, null, "pom",
                PLATFORM_VERSION_POM_EXPR));
        for (String artifactId : extraBoms) {
            expectedBoms.add(platformMemberBomCoords(artifactId));
        }
        return expectedBoms;
    }

    private void assertModel(final Path projectDir, final List<ArtifactCoords> expectedBoms,
            final List<ArtifactCoords> expectedExtensions, String platformVersion) throws IOException {
        final Model model = ModelUtils.readModel(projectDir.resolve("pom.xml"));
        assertThat(model.getProperties().getProperty(PLATFORM_GROUP_ID_POM_PROP)).isEqualTo(PLATFORM_KEY);
        assertThat(model.getProperties().getProperty(PLATFORM_ARTIFACT_ID_POM_PROP)).isEqualTo("quarkus-bom");
        assertThat(model.getProperties().getProperty(PLATFORM_VERSION_POM_PROP)).isEqualTo(platformVersion);

        // TODO the order should be predictable
        assertThat(model.getDependencyManagement().getDependencies().stream()
                .map(d -> new ArtifactCoords(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(),
                        d.getVersion()))
                .collect(Collectors.toList())).containsAll(expectedBoms);
        assertThat(model.getDependencyManagement().getDependencies().size()).isEqualTo(expectedBoms.size());

        // TODO the order should be predictable
        assertThat(model.getDependencies().stream()
                .map(d -> new ArtifactCoords(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion()))
                .collect(Collectors.toSet())).containsAll(expectedExtensions);
    }

    private static ArtifactCoords platformExtensionCoords(String artifactId) {
        return new ArtifactCoords(PLATFORM_KEY, artifactId, "jar", null);
    }

    private static ArtifactCoords platformMemberBomCoords(String artifactId) {
        return new ArtifactCoords(PLATFORM_GROUP_ID_POM_EXPR, artifactId, "pom", PLATFORM_VERSION_POM_EXPR);
    }

    private QuarkusProject getQuarkusProject(Path projectDir) {
        return QuarkusProjectHelper.getProject(projectDir, BuildTool.MAVEN);
    }

    private static Path newProjectDir(String name) {
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
