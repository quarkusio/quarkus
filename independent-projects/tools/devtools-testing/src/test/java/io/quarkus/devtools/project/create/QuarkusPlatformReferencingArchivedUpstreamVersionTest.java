package io.quarkus.devtools.project.create;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.PlatformStreamCoords;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class QuarkusPlatformReferencingArchivedUpstreamVersionTest extends MultiplePlatformBomsTestBase {

    private static final String DOWNSTREAM_PLATFORM_KEY = "io.downstream.platform";
    private static final String UPSTREAM_PLATFORM_KEY = "io.upstream.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("downstream.registry.test")
                .recognizedQuarkusVersions("*-downstream")
                // platform key
                .newPlatform(DOWNSTREAM_PLATFORM_KEY)
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.4 release
                .newRelease("2.0.4-downstream")
                .quarkusVersion("2.2.2-downstream")
                .upstreamQuarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                // foo platform member
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "2.0.4-downstream").release().stream().platform()
                .newStream("1.0")
                // 1.0.4 release
                .newRelease("1.0.4-downstream")
                .quarkusVersion("1.1.1-downstream")
                .upstreamQuarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                // foo platform member
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "1.0.4-downstream").release()
                .newMember("acme-e-bom").addExtension("io.acme", "ext-e", "1.0.4-downstream").release()
                .stream().platform().registry()
                .newNonPlatformCatalog("1.1.1-downstream").addExtension("io.acme", "ext-d", "4.0-downstream").registry()
                .clientBuilder()
                .newRegistry("upstream.registry.test")
                // platform key
                .newPlatform(UPSTREAM_PLATFORM_KEY)
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.5 release
                .newRelease("2.0.5")
                .quarkusVersion("2.2.5")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "2.0.5").release()
                .newMember("acme-e-bom").addExtension("io.acme", "ext-e", "2.0.5").release()
                .newMember("acme-bar-bom").addExtension("io.acme", "ext-b", "2.0.5").release().stream()
                // 2.0.4 release
                .newArchivedRelease("2.0.4")
                .quarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "2.0.4").release()
                .newMember("acme-e-bom").addExtension("io.acme", "ext-e", "2.0.4").release()
                .newMember("acme-bar-bom").addExtension("io.acme", "ext-b", "2.0.4").release().stream().platform()
                // 1.0 STREAM
                .newStream("1.0")
                .newRelease("1.0.5")
                .quarkusVersion("1.1.5")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "1.0.5").addExtension("io.acme", "ext-e", "1.0.5")
                .release()
                .newMember("acme-bar-bom").addExtension("io.acme", "ext-b", "1.0.5").release()
                .stream()
                .newArchivedRelease("1.0.4")
                .quarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "1.0.4").addExtension("io.acme", "ext-e", "1.0.4")
                .release()
                .newMember("acme-bar-bom").addExtension("io.acme", "ext-b", "1.0.4").release()
                .stream().platform().registry()
                .newNonPlatformCatalog("2.2.2").addExtension("io.acme", "ext-c", "5.1").addExtension("io.acme", "ext-d", "6.0")
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return DOWNSTREAM_PLATFORM_KEY;
    }

    @Test
    public void addExtensionsFromAlreadyImportedPlatform() throws Exception {
        final Path projectDir = newProjectDir("downstream-upstream-platform");
        createProject(projectDir, Arrays.asList("ext-a"));

        assertModel(projectDir,
                toPlatformBomCoords("acme-foo-bom"),
                Arrays.asList(new ArtifactCoords("io.acme", "ext-a", null)),
                "2.0.4-downstream");

        addExtensions(projectDir, Arrays.asList("ext-b", "ext-c", "ext-d", "ext-e"));
        assertModel(projectDir,
                Arrays.asList(mainPlatformBom(), platformMemberBomCoords("acme-foo-bom"),
                        new ArtifactCoords(UPSTREAM_PLATFORM_KEY, "acme-bar-bom", "pom", "2.0.4"),
                        new ArtifactCoords(UPSTREAM_PLATFORM_KEY, "acme-e-bom", "pom", "2.0.4")),
                Arrays.asList(new ArtifactCoords("io.acme", "ext-a", null),
                        new ArtifactCoords("io.acme", "ext-b", null),
                        new ArtifactCoords("io.acme", "ext-e", null),
                        new ArtifactCoords("io.acme", "ext-c", "jar", "5.1"),
                        new ArtifactCoords("io.acme", "ext-d", "jar", "6.0")),
                "2.0.4-downstream");
    }

    @Test
    public void createWithExtensionsFromDifferentPlatforms() throws Exception {
        final Path projectDir = newProjectDir("create-downstream-upstream-platform");
        createProject(projectDir, Arrays.asList("ext-a", "ext-b"));

        assertModel(projectDir,
                Arrays.asList(mainPlatformBom(), platformMemberBomCoords("acme-foo-bom"),
                        new ArtifactCoords(UPSTREAM_PLATFORM_KEY, "acme-bar-bom", "pom", "2.0.4")),
                Arrays.asList(new ArtifactCoords("io.acme", "ext-a", null), new ArtifactCoords("io.acme", "ext-b", null)),
                "2.0.4-downstream");
    }

    @Test
    public void createPreferringOlderStreamToNewerStreamCoveringLessExtensions() throws Exception {
        final Path projectDir = newProjectDir("create-downstream-upstream-platform");
        createProject(projectDir, Arrays.asList("ext-a", "ext-b", "ext-e"));

        assertModel(projectDir,
                Arrays.asList(mainPlatformBom(), platformMemberBomCoords("acme-foo-bom"), platformMemberBomCoords("acme-e-bom"),
                        new ArtifactCoords(UPSTREAM_PLATFORM_KEY, "acme-bar-bom", "pom", "1.0.4")),
                Arrays.asList(new ArtifactCoords("io.acme", "ext-a", null), new ArtifactCoords("io.acme", "ext-b", null),
                        new ArtifactCoords("io.acme", "ext-e", null)),
                "1.0.4-downstream");
    }

    @Test
    public void createUsingStream2_0() throws Exception {
        final Path projectDir = newProjectDir("created-using-downstream-stream");
        createProject(projectDir, new PlatformStreamCoords(DOWNSTREAM_PLATFORM_KEY, "2.0"),
                Arrays.asList("ext-a", "ext-b", "ext-e"));

        assertModel(projectDir,
                Arrays.asList(mainPlatformBom(), platformMemberBomCoords("acme-foo-bom"),
                        new ArtifactCoords(UPSTREAM_PLATFORM_KEY, "acme-e-bom", "pom", "2.0.4"),
                        new ArtifactCoords(UPSTREAM_PLATFORM_KEY, "acme-bar-bom", "pom", "2.0.4")),
                Arrays.asList(new ArtifactCoords("io.acme", "ext-a", null), new ArtifactCoords("io.acme", "ext-b", null),
                        new ArtifactCoords("io.acme", "ext-e", null)),
                "2.0.4-downstream");
    }
}
