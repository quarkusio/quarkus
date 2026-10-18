package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.PlatformStreamCoords;

public class QuarkusPlatformArchivedStreamSelectionTest extends MultiplePlatformBomsTestBase {

    private static final String ACME_PLATFORM_KEY = "org.acme.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("registry.acme.org")
                // platform key
                .newPlatform(ACME_PLATFORM_KEY)
                // 3.0 STREAM
                .newStream("3.0")
                // 3.0.5 release
                .newRelease("3.0.5")
                .quarkusVersion("3.0.5")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-zoo-bom").addExtension("org.acme", "acme-rabbit", "3.0.5")
                .release().stream().platform()
                // 2.0 STREAM
                .newArchivedStream("2.0")
                .newRelease("2.0.5")
                .quarkusVersion("2.0.5")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-zoo-bom").addExtension("org.acme", "acme-rabbit", "2.0.5")
                .release().stream()
                .newRelease("2.0.4")
                .quarkusVersion("2.0.4")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-zoo-bom").addExtension("org.acme", "acme-rabbit", "2.0.4")
                .release().stream()
                .newRelease("2.0.3")
                .quarkusVersion("2.0.3")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-zoo-bom")
                .addExtension("org.acme", "acme-rabbit", "2.0.3")
                .addExtension("org.acme", "acme-giraffe", "2.0.3")
                .release().stream().platform()
                // 1.0 STREAM
                .newStream("1.0")
                .newRelease("1.0.5")
                .quarkusVersion("1.0.5")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-zoo-bom")
                .addExtension("org.acme", "acme-rabbit", "1.0.5")
                .addExtension("org.acme", "acme-giraffe", "1.0.5")
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return ACME_PLATFORM_KEY;
    }

    @Test
    public void testLatestRecommendedStream() throws Exception {
        final Path projectDir = newProjectDir("latest-recommended-stream-selection");
        createProject(projectDir, List.of("acme-rabbit"));

        assertModel(projectDir,
                toPlatformBomCoords("acme-zoo-bom"),
                List.of(ArtifactCoords.jar("org.acme", "acme-rabbit", null)),
                "3.0.5");
    }

    @Test
    public void testLatestRecommendedMatchingStreamRelease() throws Exception {
        final Path projectDir = newProjectDir("latest-recommended-matching-stream");
        createProject(projectDir, List.of("acme-rabbit", "acme-giraffe"));

        assertModel(projectDir,
                toPlatformBomCoords("acme-zoo-bom"),
                List.of(ArtifactCoords.jar("org.acme", "acme-rabbit", null),
                        ArtifactCoords.jar("org.acme", "acme-giraffe", null)),
                "1.0.5");
    }

    @Test
    public void testArchivedStreamSelection() throws Exception {
        final Path projectDir = newProjectDir("archived-stream-selection");
        createProject(projectDir, new PlatformStreamCoords(ACME_PLATFORM_KEY, "2.0"),
                List.of("acme-rabbit"));

        assertModel(projectDir,
                toPlatformBomCoords("acme-zoo-bom"),
                List.of(ArtifactCoords.jar("org.acme", "acme-rabbit", null)),
                "2.0.5");
    }

    /**
     * This one may seem like an edge case. This test makes sure a release that includes an extension
     * that was removed in later releases in the same stream still gets selected when that extension
     * is requested by a user.
     *
     * @throws Exception in case of an error
     */
    @Test
    public void testArchivedMatchingStreamRelease() throws Exception {
        final Path projectDir = newProjectDir("archived-stream-selection");
        createProject(projectDir, new PlatformStreamCoords(ACME_PLATFORM_KEY, "2.0"),
                List.of("acme-rabbit", "acme-giraffe"));

        assertModel(projectDir,
                toPlatformBomCoords("acme-zoo-bom"),
                List.of(ArtifactCoords.jar("org.acme", "acme-rabbit", null),
                        ArtifactCoords.jar("org.acme", "acme-giraffe", null)),
                "2.0.3");
    }
}
