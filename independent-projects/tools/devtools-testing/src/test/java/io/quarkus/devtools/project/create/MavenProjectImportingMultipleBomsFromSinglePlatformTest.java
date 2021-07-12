package io.quarkus.devtools.project.create;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.ArtifactCoords;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MavenProjectImportingMultipleBomsFromSinglePlatformTest extends MultiplePlatformBomsTestBase {

    private static final String MAIN_PLATFORM_KEY = "org.acme.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("registry.acme.org")
                // platform key
                .newPlatform(MAIN_PLATFORM_KEY)
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

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return MAIN_PLATFORM_KEY;
    }

    @Test
    public void createWithNonPlatformExtensionCompatibleWithTheOldestQuarkusVersion() throws Exception {
        final Path projectDir = newProjectDir("non-platform-extension-oldest-quarkus-version");
        createProject(projectDir, Arrays.asList("acme-foo", "other-extension", "other-five-zero"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:5.0"));
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-five-zero:5.0"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom"), expectedExtensions, "1.0.0");
    }

    @Test
    public void createWithNonPlatformExtensionCompatibleWithTheLatestQuarkusVersion() throws Exception {
        final Path projectDir = newProjectDir("non-platform-extension-latest-quarkus-version");
        createProject(projectDir, Arrays.asList("acme-foo", "other-extension", "other-six-zero"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:6.0"));
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-six-zero:6.0"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom"), expectedExtensions, "2.0.4");
    }

    @Test
    public void createWithNonPlatformExtensionCompatibleWithQuarkusVersion112() throws Exception {
        final Path projectDir = newProjectDir("non-platform-extension-quarkus-version-112");
        createProject(projectDir, Arrays.asList("acme-foo", "other-extension", "other-five-one"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:5.1"));
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-five-one:5.1"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom"), expectedExtensions, "1.0.1");
    }

    @Test
    public void createWithExtensionsPresentInTheLatestReleaseOfTheLatestStream() throws Exception {
        final Path projectDir = newProjectDir("latest-release-latest-stream");
        createProject(projectDir, Arrays.asList("acme-foo", "other-extension"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:6.0"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom"), expectedExtensions, "2.0.4");
    }

    @Test
    public void createWithExtensionsPresentInTheLatestReleaseOfAnOldStream() throws Exception {
        final Path projectDir = newProjectDir("latest-release-old-stream");
        createProject(projectDir, Arrays.asList("acme-baz", "acme-foo", "other-extension"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo", "acme-baz");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:5.1"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom", "acme-baz-bom"), expectedExtensions, "1.0.1");
    }

    @Test
    public void createWithExtensionPresentInTheOldReleaseOfAnOldStream() throws Exception {
        final Path projectDir = newProjectDir("old-release-old-stream");
        createProject(projectDir, Arrays.asList("acme-bar", "acme-foo", "other-extension"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-foo", "acme-bar");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-extension:5.0"));
        assertModel(projectDir, toPlatformBomCoords("acme-foo-bom", "acme-bar-bom"),
                expectedExtensions, "1.0.0");
    }

    @Test
    public void addExtensionAndImportMemberBom() throws Exception {
        final Path projectDir = newProjectDir("add-extension-import-bom");
        createProject(projectDir, Arrays.asList("other-five-one"));

        assertModel(projectDir, toPlatformBomCoords(), Arrays.asList(ArtifactCoords.fromString("org.other:other-five-one:5.1")),
                "1.0.1");

        addExtensions(projectDir, Arrays.asList("acme-baz"));

        final List<ArtifactCoords> expectedExtensions = toPlatformExtensionCoords("acme-baz");
        expectedExtensions.add(ArtifactCoords.fromString("org.other:other-five-one:5.1"));
        assertModel(projectDir, toPlatformBomCoords("acme-baz-bom"), expectedExtensions, "1.0.1");
    }

    @Test
    public void attemptCreateWithIncompatibleExtensions() throws Exception {
        final Path projectDir = newProjectDir("create-with-incompatible-extensions");
        assertThat(createProject(projectDir, Arrays.asList("acme-bar", "other-five-one")).isSuccess()).isFalse();
    }

    @Test
    public void attemptAddingExtensionFromIncompatibleMemberBom() throws Exception {
        final Path projectDir = newProjectDir("add-extension-incompatible-member-bom");
        createProject(projectDir, Arrays.asList("other-five-one"));

        assertModel(projectDir, toPlatformBomCoords(), Arrays.asList(ArtifactCoords.fromString("org.other:other-five-one:5.1")),
                "1.0.1");

        assertThat(addExtensions(projectDir, Arrays.asList("acme-bar")).isSuccess()).isFalse();
    }
}
