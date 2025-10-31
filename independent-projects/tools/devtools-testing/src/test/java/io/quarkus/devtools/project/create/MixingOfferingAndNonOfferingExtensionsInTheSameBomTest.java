package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;

public class MixingOfferingAndNonOfferingExtensionsInTheSameBomTest extends MultiplePlatformBomsTestBase {

    private static final String DOWNSTREAM_PLATFORM_KEY = "io.downstream.platform";
    private static final String UPSTREAM_PLATFORM_KEY = "io.upstream.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("downstream.registry.test")
                .recognizedQuarkusVersions("*downstream")
                .setOffering("offering-a")
                // platform key
                .newPlatform(DOWNSTREAM_PLATFORM_KEY)
                .newStream("1.1")
                // 1.1.1 release
                .newRelease("1.1.1.downstream")
                .quarkusVersion("1.1.1.downstream")
                .upstreamQuarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                .addExtensionWithMetadata("io.quarkus", "quarkus-rest", "1.1.1.downstream",
                        Map.of("offering-a-support", List.of("supported")))
                .addExtension("io.quarkus", "quarkus-magic", "1.1.1.downstream")
                .release()
                // foo platform member
                .newMember("acme-a-bom")
                .addExtensionWithMetadata("io.acme", "ext-a", "1.1.1.downstream",
                        Map.of("offering-a-support", List.of("supported")))
                .addExtension("io.acme", "ext-b", "1.1.1.downstream")
                .release()
                .stream().platform().registry()
                .clientBuilder()
                .newRegistry("upstream.registry.test")
                // platform key
                .newPlatform(UPSTREAM_PLATFORM_KEY)
                // 1.1 STREAM
                .newStream("1.1")
                .newRelease("1.1.1")
                .quarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                .addExtension("io.quarkus", "quarkus-rest", "1.1.1")
                .addExtension("io.quarkus", "quarkus-magic", "1.1.1")
                .release()
                .newMember("acme-a-bom")
                .addExtension("io.acme", "ext-a", "1.1.1")
                .addExtension("io.acme", "ext-b", "1.1.1")
                .release()
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return DOWNSTREAM_PLATFORM_KEY;
    }

    @Test
    public void downstreamCoreBomOnlyForSupportedExtension() throws Exception {
        final Path projectDir = newProjectDir("downstream-core-bom-only-for-supported-extension");
        createProject(projectDir, List.of("quarkus-rest"));

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", null)),
                "1.1.1.downstream");
    }

    @Test
    public void downstreamBomUsedForNotSupportedExtensions() throws Exception {
        final Path projectDir = newProjectDir("downstream-bom-used-for-unsupported-extensions");
        createProject(projectDir, List.of("quarkus-rest", "quarkus-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", null),
                        ArtifactCoords.jar("io.quarkus", "quarkus-magic", null)),
                "1.1.1.downstream");
    }

    @Test
    public void upstreamCoreBomOnly() throws Exception {
        final Path projectDir = newProjectDir("upstream-core-bom-only-project");
        createProject(projectDir, List.of("quarkus-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-magic", null)),
                Map.of("quarkus.platform.group-id", UPSTREAM_PLATFORM_KEY,
                        "quarkus.platform.artifact-id", "quarkus-bom",
                        "quarkus.platform.version", "1.1.1"));
    }

    @Test
    public void downstreamBomsOnly() throws Exception {
        final Path projectDir = newProjectDir("downstream-bom-only-project");
        createProject(projectDir, List.of("ext-a", "ext-b"));

        assertModel(projectDir,
                List.of(mainPlatformBom(),
                        platformMemberBomCoords("acme-a-bom")),
                List.of(ArtifactCoords.jar("io.acme", "ext-a", null),
                        ArtifactCoords.jar("io.acme", "ext-b", null)),
                "1.1.1.downstream");
    }

    @Test
    public void upstreamBomsOnly() throws Exception {
        final Path projectDir = newProjectDir("upstream-bom-only-project");
        createProject(projectDir, List.of("ext-b"));

        assertModel(projectDir,
                List.of(mainPlatformBom(),
                        platformMemberBomCoords("acme-a-bom")),
                List.of(ArtifactCoords.jar("io.acme", "ext-b", null)),
                Map.of("quarkus.platform.group-id", UPSTREAM_PLATFORM_KEY,
                        "quarkus.platform.artifact-id", "quarkus-bom",
                        "quarkus.platform.version", "1.1.1"));
    }
}
