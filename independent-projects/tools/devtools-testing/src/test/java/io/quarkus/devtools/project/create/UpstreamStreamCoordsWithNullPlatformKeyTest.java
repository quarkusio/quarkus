package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.PlatformStreamCoords;

public class UpstreamStreamCoordsWithNullPlatformKeyTest extends MultiplePlatformBomsTestBase {

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
                .newStream("1.0")
                // 1.0.4 release
                .newRelease("1.1.1-downstream")
                .quarkusVersion("1.1.1-downstream")
                .upstreamQuarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                // foo platform member
                .newMember("acme-a-bom").addExtension("io.acme", "ext-a", "1.1.1-downstream").release()
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
                .newStream("1.0")
                .newRelease("1.1.1")
                .quarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .newMember("acme-a-bom").addExtension("io.acme", "ext-a", "1.1.1")
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    @Override
    protected String getMainPlatformKey() {
        return UPSTREAM_PLATFORM_KEY;
    }

    @Test
    public void createUsingStream2_0() throws Exception {
        final Path projectDir = newProjectDir("created-using-upstream-2.0");
        createProject(projectDir, new PlatformStreamCoords(null, "2.0"), List.of("ext-a"));

        assertModel(projectDir,
                List.of(mainPlatformBom(), platformMemberBomCoords("acme-a-bom")),
                List.of(ArtifactCoords.jar("io.acme", "ext-a", null)),
                "2.0.5");
    }
}
