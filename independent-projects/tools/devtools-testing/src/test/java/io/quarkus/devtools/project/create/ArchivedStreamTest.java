package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.PlatformStreamCoords;

public class ArchivedStreamTest extends MultiplePlatformBomsTestBase {

    private static final String PLATFORM_KEY = "io.test.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("registry.test.io")
                // platform key
                .newPlatform(PLATFORM_KEY)
                .newStream("1.0")
                // 1.0.4 release
                .newRelease("1.1.1")
                .quarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                // foo platform member
                .newMember("acme-a-bom").addExtension("ext-a").release()
                .stream().platform()
                .newArchivedStream("0.5")
                .newArchivedRelease("0.5.1")
                .quarkusVersion("0.5.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                // foo platform member
                .newMember("acme-a-bom").addExtension("ext-a").release()
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    @Override
    protected String getMainPlatformKey() {
        return PLATFORM_KEY;
    }

    @Test
    public void createUsingStream2_0() throws Exception {
        final Path projectDir = newProjectDir("created-using-archive-stream-0.5");
        createProject(projectDir, new PlatformStreamCoords(null, "0.5"), List.of("ext-a"));

        assertModel(projectDir,
                List.of(mainPlatformBom(), platformMemberBomCoords("acme-a-bom")),
                List.of(ArtifactCoords.jar("io.test.platform", "ext-a", null)),
                "0.5.1");
    }
}
