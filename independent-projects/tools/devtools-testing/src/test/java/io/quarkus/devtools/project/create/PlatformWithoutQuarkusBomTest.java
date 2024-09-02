package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;

public class PlatformWithoutQuarkusBomTest extends MultiplePlatformBomsTestBase {

    private static final String MAIN_PLATFORM_KEY = "org.acme.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry.acme.org
                .newRegistry("registry.acme.org")
                .newPlatform(MAIN_PLATFORM_KEY)
                .newStream("7.0")
                .newRelease("7.0.7")
                .quarkusVersion("2.2.2")
                .newMember("acme-magic-bom")
                .addExtension("acme-magic")
                .release()
                .stream().platform()
                .registry()
                .clientBuilder()
                // Main Quarkus registry
                .newRegistry("registry.quarkus.org")
                // platform key
                .newPlatform("org.quarkus.platform")
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.4 release
                .newRelease("2.0.4")
                .quarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                .addExtension("quarkus-magic")
                .release()
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return "org.quarkus.platform";
    }

    @Test
    public void testDeafultCodestart() throws Exception {
        final Path projectDir = newProjectDir("created-platform-wo-quarkus-bom");
        createProject(projectDir, List.of());

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-resteasy-reactive", null)),
                "2.0.4");
    }

    @Test
    public void testQuarkusPlatformMagic() throws Exception {
        final Path projectDir = newProjectDir("created-platform-wo-quarkus-bom");
        createProject(projectDir, List.of("quarkus-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("org.quarkus.platform", "quarkus-magic", null)),
                "2.0.4");
    }

    @Test
    public void testAcmePlatformMagic() throws Exception {
        final Path projectDir = newProjectDir("created-platform-wo-quarkus-bom");
        createProject(projectDir, List.of("acme-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom(), ArtifactCoords.pom(MAIN_PLATFORM_KEY, "acme-magic-bom", "7.0.7")),
                List.of(ArtifactCoords.jar("org.acme.platform", "acme-magic", null)),
                "2.0.4");
    }
}
