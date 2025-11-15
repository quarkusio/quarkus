package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;

public class RegistryOfferingFilteringConfig3Test extends MultiplePlatformBomsTestBase {

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
                .setOffering("offering-c")
                // platform key
                .newPlatform(DOWNSTREAM_PLATFORM_KEY)
                .newStream("1.1")
                // 1.1.1 release
                .newRelease("1.1.1.downstream")
                .quarkusVersion("1.1.1.downstream")
                .upstreamQuarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                // foo platform member
                .newMember("acme-a-bom")
                .addExtensionWithMetadata("io.acme", "ext-a", "1.1.1.downstream",
                        Map.of("offering-a-support", List.of("supported")))
                .release()
                .newMember("acme-b-bom")
                .addExtensionWithMetadata("io.acme", "ext-b", "1.1.1.downstream",
                        Map.of("offering-a-support", List.of("supported"),
                                "offering-b-support", List.of("supported")))
                .release()
                .newMember("acme-c-bom")
                .addExtensionWithMetadata("io.acme", "ext-c", "1.1.1.downstream",
                        Map.of("offering-a-support", List.of("supported"),
                                "offering-b-support", List.of("supported"),
                                "offering-c-support", List.of("supported")))
                .release()
                .stream().platform().registry()
                .clientBuilder()
                .newRegistry("upstream.registry.test")
                // platform key
                .newPlatform(UPSTREAM_PLATFORM_KEY)
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.5 release
                .newRelease("2.0.5")
                .quarkusVersion("2.0.5")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-a-bom").addExtension("io.acme", "ext-a", "2.0.5").release().stream().platform()
                // 1.0 STREAM
                .newStream("1.1")
                .newRelease("1.1.1")
                .quarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-a-bom").addExtension("io.acme", "ext-a", "1.1.1")
                .release()
                .newMember("acme-b-bom").addExtension("io.acme", "ext-b", "1.1.1")
                .release()
                .newMember("acme-c-bom").addExtension("io.acme", "ext-c", "1.1.1")
                .release()
                .newMember("acme-d-bom").addExtension("io.acme", "ext-d", "1.1.1")
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return DOWNSTREAM_PLATFORM_KEY;
    }

    @Test
    public void createOfferingA() throws Exception {
        final Path projectDir = newProjectDir("offering-a-based-project");
        createProject(projectDir, List.of("ext-a", "ext-b", "ext-c", "ext-d"));

        assertModel(projectDir,
                List.of(mainPlatformBom(),
                        platformMemberBomCoords("acme-c-bom"),
                        ArtifactCoords.pom(UPSTREAM_PLATFORM_KEY, "acme-a-bom", "1.1.1"),
                        ArtifactCoords.pom(UPSTREAM_PLATFORM_KEY, "acme-b-bom", "1.1.1"),
                        ArtifactCoords.pom(UPSTREAM_PLATFORM_KEY, "acme-d-bom", "1.1.1")),
                List.of(ArtifactCoords.jar("io.acme", "ext-a", null),
                        ArtifactCoords.jar("io.acme", "ext-b", null),
                        ArtifactCoords.jar("io.acme", "ext-c", null),
                        ArtifactCoords.jar("io.acme", "ext-d", null)),
                "1.1.1.downstream");
    }
}
