package io.quarkus.devtools.project.create;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.util.PlatformArtifacts;

/**
 * This test mimics the scenario of registry.quarkus.io recommending io.quarkus.platform
 * while there is the io.quarkus one, that is not known to registry.quarkus.io but is still resolvable
 * by the Maven registry client configured to pull data from registry.quarkus.io.
 *
 * It's a weird setup, but it's been working like this from the beginning. We may want to re-work this
 * to something more reasonable.
 *
 * The test makes sure that the preferred target platform, provided by a user as input (such the io.quarkus one),
 * is prioritized over what the configured registry recommends.
 */
public class PreferredPlatformKeysNotProvidedByRegistryTest extends MultiplePlatformBomsTestBase {

    private static final String MAIN_PLATFORM_KEY = "org.acme.platform";

    @BeforeAll
    public static void setup() throws Exception {

        final String quarkusCoreVersion = "1.1.2";
        final ArtifactCoords ioQuarkusBomCoords = ArtifactCoords.pom("io.quarkus", "quarkus-bom", quarkusCoreVersion);
        var ioQuarkusPlatform = ExtensionCatalog.builder()
                .setBom(ioQuarkusBomCoords)
                .setId(PlatformArtifacts.ensureCatalogArtifact(ioQuarkusBomCoords).toString())
                .setQuarkusCoreVersion(quarkusCoreVersion)
                .setPlatform(true);
        TestRegistryClientBuilder.initMainPlatformMetadata(ioQuarkusPlatform);
        ioQuarkusPlatform.addExtension(Extension.builder()
                .setName("Quarkus Core")
                .setGroupId("io.quarkus")
                .setArtifactId("quarkus-core")
                .setVersion(quarkusCoreVersion)
                .setOrigins(List.of(ioQuarkusPlatform)));
        ioQuarkusPlatform.addExtension(Extension.builder()
                .setName("Acme Magic")
                .setGroupId("io.quarkus")
                .setArtifactId("acme-magic")
                .setVersion(quarkusCoreVersion)
                .setOrigins(List.of(ioQuarkusPlatform)));
        ioQuarkusPlatform.addExtension(Extension.builder()
                .setName("Acme Foo")
                .setGroupId("io.quarkus")
                .setArtifactId("acme-foo")
                .setVersion(quarkusCoreVersion)
                .setOrigins(List.of(ioQuarkusPlatform)));

        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("registry.acme.org")
                // platform key
                .newPlatform(MAIN_PLATFORM_KEY)
                // 1.0 STREAM
                .newStream("1.0")
                // 1.0.1 release
                .newRelease("1.0.1")
                .quarkusVersion(quarkusCoreVersion)
                .addCoreMember().release()
                .newMember("acme-foo-bom").addExtension("io.quarkus", "acme-foo", quarkusCoreVersion).release()
                .registry()
                .newNonPlatformCatalog(quarkusCoreVersion)
                .addExtension("org.acme", "acme-quarkus-other", "5.5.5")
                .registry()
                .enableMavenResolver()
                .clientBuilder()
                // external io.quarkus
                .installExternalPlatform(ioQuarkusPlatform)
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return MAIN_PLATFORM_KEY;
    }

    @Test
    public void testRegistryRecommendation() throws Exception {
        final ExtensionCatalog catalog = ExtensionCatalogResolver.builder().build().resolveExtensionCatalog();
        assertThat(List.of(
                ArtifactCoords.jar("io.quarkus", "quarkus-core", "1.1.2"),
                ArtifactCoords.jar("io.quarkus", "acme-foo", "1.1.2"),
                ArtifactCoords.jar("org.acme", "acme-quarkus-other", "5.5.5")))
                .containsExactlyInAnyOrderElementsOf(
                        catalog.getExtensions().stream().map(Extension::getArtifact).collect(Collectors.toList()));

        final Path projectDir = newProjectDir("preferred-platform-keys-registry-recommendation");
        createProject(projectDir, List.of("acme-foo"));
        assertModel(projectDir,
                List.of(mainPlatformBom(), platformMemberBomCoords("acme-foo-bom")),
                List.of(ArtifactCoords.jar("io.quarkus", "acme-foo", null)),
                Map.of("quarkus.platform.group-id", MAIN_PLATFORM_KEY,
                        "quarkus.platform.artifact-id", "quarkus-bom",
                        "quarkus.platform.version", "1.0.1"));
    }

    @Test
    public void testIoQuarkusBom() throws Exception {
        final List<ArtifactCoords> preferredBoms = List.of(ArtifactCoords.pom("io.quarkus", "quarkus-bom", "1.1.2"));
        final ExtensionCatalog catalog = ExtensionCatalogResolver.builder().build().resolveExtensionCatalog(
                preferredBoms);
        assertThat(List.of(
                ArtifactCoords.jar("io.quarkus", "quarkus-core", "1.1.2"),
                ArtifactCoords.jar("io.quarkus", "acme-magic", "1.1.2"),
                ArtifactCoords.jar("io.quarkus", "acme-foo", "1.1.2"),
                ArtifactCoords.jar("org.acme", "acme-quarkus-other", "5.5.5")))
                .containsExactlyInAnyOrderElementsOf(
                        catalog.getExtensions().stream().map(Extension::getArtifact).collect(Collectors.toList()));

        final Path projectDir = newProjectDir("preferred-platform-keys-io-quarkus");
        createProject(projectDir, preferredBoms, List.of("acme-foo"));
        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("io.quarkus", "acme-foo", null)),
                Map.of("quarkus.platform.group-id", "io.quarkus",
                        "quarkus.platform.artifact-id", "quarkus-bom",
                        "quarkus.platform.version", "1.1.2"));
    }
}
