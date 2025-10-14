package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;

public class CreateProjectWithNonPlatformExtensionTest extends MultiplePlatformBomsTestBase {

    private static final String PLATFORM_KEY = "org.acme.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("registry.acme.org")
                // platform key
                .newPlatform(PLATFORM_KEY)
                .newStream("1.1")
                // 1.1.1 release
                .newRelease("1.1.1")
                .quarkusVersion("1.1.1")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().release()
                .stream().platform().registry()
                .newNonPlatformCatalog("1.1.1")
                .addExtension("org.bla", "bla-magic", "5.5.5")
                // this is just to make sure quarkus-core (by accident) included in a non-platform catalog does not mislead the tools
                .addExtension("io.quarkus", "quarkus-core", "5.5.5")
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return PLATFORM_KEY;
    }

    @Test
    public void projectWithNonPlatformExtensionOnly() throws Exception {
        final Path projectDir = newProjectDir("project-non-platform-ext");
        createProject(projectDir, List.of("bla-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("org.bla", "bla-magic", "5.5.5")),
                "1.1.1");
    }
}
