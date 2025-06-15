package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;

public class QuarkusPlatformReferencingUpstreamVersionTest extends MultiplePlatformBomsTestBase {

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
                .addCoreMember().release()
                // foo platform member
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "2.0.4-downstream").release().stream().platform()
                .newStream("1.0")
                // 1.0.4 release
                .newRelease("1.0.4-downstream")
                .quarkusVersion("1.1.1-downstream")
                .upstreamQuarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
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
                // 2.0.4 release
                .newRelease("2.0.4")
                .quarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "2.0.4").release()
                .newMember("acme-e-bom").addExtension("io.acme", "ext-e", "2.0.4").release()
                .newMember("acme-bar-bom").addExtension("io.acme", "ext-b", "2.0.4").release().stream().platform()
                .newStream("1.0")
                .newRelease("1.0.4")
                .quarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
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

    @Override
    protected String getMainPlatformKey() {
        return DOWNSTREAM_PLATFORM_KEY;
    }

    @Test
    public void addExtensionsFromAlreadyImportedPlatform() throws Exception {
        final Path projectDir = newProjectDir("downstream-upstream-platform");
        createProject(projectDir, Arrays.asList("ext-a"));

        assertModel(projectDir,
                toPlatformBomCoords("acme-foo-bom"),
                List.of(ArtifactCoords.jar("io.acme", "ext-a", null)),
                "2.0.4-downstream");

        addExtensions(projectDir, Arrays.asList("ext-b", "ext-c", "ext-d", "ext-e"));
        assertModel(projectDir,
                List.of(mainPlatformBom(), platformMemberBomCoords("acme-foo-bom"),
                        ArtifactCoords.pom(UPSTREAM_PLATFORM_KEY, "acme-bar-bom", "2.0.4"),
                        ArtifactCoords.pom(UPSTREAM_PLATFORM_KEY, "acme-e-bom", "2.0.4")),
                List.of(ArtifactCoords.jar("io.acme", "ext-a", null),
                        ArtifactCoords.jar("io.acme", "ext-b", null),
                        ArtifactCoords.jar("io.acme", "ext-e", null),
                        ArtifactCoords.jar("io.acme", "ext-c", "5.1"),
                        ArtifactCoords.jar("io.acme", "ext-d", "6.0")),
                "2.0.4-downstream");
    }

    @Test
    public void createWithExtensionsFromDifferentPlatforms() throws Exception {
        final Path projectDir = newProjectDir("create-downstream-upstream-platform");
        createProject(projectDir, Arrays.asList("ext-a", "ext-b"));

        assertModel(projectDir,
                List.of(mainPlatformBom(), platformMemberBomCoords("acme-foo-bom"),
                        ArtifactCoords.pom(UPSTREAM_PLATFORM_KEY, "acme-bar-bom", "2.0.4")),
                List.of(ArtifactCoords.jar("io.acme", "ext-a", null),
                        ArtifactCoords.jar("io.acme", "ext-b", null)),
                "2.0.4-downstream");
    }

    @Test
    public void createPreferringOlderStreamToNewerStreamCoveringLessExtensions() throws Exception {
        final Path projectDir = newProjectDir("create-downstream-upstream-platform");
        createProject(projectDir, Arrays.asList("ext-a", "ext-b", "ext-e"));

        assertModel(projectDir,
                List.of(mainPlatformBom(), platformMemberBomCoords("acme-foo-bom"), platformMemberBomCoords("acme-e-bom"),
                        ArtifactCoords.pom(UPSTREAM_PLATFORM_KEY, "acme-bar-bom", "1.0.4")),
                List.of(ArtifactCoords.jar("io.acme", "ext-a", null),
                        ArtifactCoords.jar("io.acme", "ext-b", null),
                        ArtifactCoords.jar("io.acme", "ext-e", null)),
                "1.0.4-downstream");
    }
}
