package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;

public class QuarkusPlatformBasedOnArchivedQuarkusCoreTest extends MultiplePlatformBomsTestBase {

    private static final String ACME_PLATFORM_KEY = "org.acme.platform";
    private static final String OTHER_PLATFORM_KEY = "org.other.platform";

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
                .newStream("5.0")
                // 3.0.5 release
                .newRelease("5.0.5")
                .quarkusVersion("3.0.5")
                .newMember("acme-zoo-bom").addExtension("org.acme", "acme-rabbit", "5.0.5")
                .release().stream().platform()
                // not promoted later release
                .newArchivedStream("6.0")
                .newRelease("6.0.0")
                .quarkusVersion("4.0.0")
                .newMember("acme-zoo-bom").addExtension("org.acme", "acme-rabbit", "6.0.0")
                .release().stream().platform()
                .registry().clientBuilder()
                // Other registry
                .newRegistry("registry.other.org")
                .newPlatform(OTHER_PLATFORM_KEY)
                .newStream("4.0")
                .newRelease("4.0.0")
                .quarkusVersion("4.0.0")
                .addCoreMember()
                .addExtension("quarkus-rest")
                .addExtension("quarkus-magic")
                .release().stream().platform()
                // 3.0 STREAM
                .newArchivedStream("3.0")
                .newRelease("3.0.5")
                .quarkusVersion("3.0.5")
                .addCoreMember()
                .addExtension("quarkus-rest")
                .addExtension("quarkus-magic")
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return OTHER_PLATFORM_KEY;
    }

    @Test
    public void testLatestRecommendedQuarkusCore() throws Exception {
        final Path projectDir = newProjectDir("latest-other-platform");
        createProject(projectDir, List.of("quarkus-rest"));

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar(OTHER_PLATFORM_KEY, "quarkus-rest", null)),
                "4.0.0");
    }

    @Test
    public void testLatestRecommendedAcmeRabbit() throws Exception {
        final Path projectDir = newProjectDir("latest-acme-platform");
        createProject(projectDir, List.of("rabbit"));

        assertModel(projectDir,
                List.of(mainPlatformBom(), ArtifactCoords.pom(ACME_PLATFORM_KEY, "acme-zoo-bom", "5.0.5")),
                List.of(ArtifactCoords.jar("org.acme", "acme-rabbit", null)),
                "3.0.5");
    }

    @Test
    public void testLatestRecommendedQuarkusCoreAndAcmeRabbitCombination() throws Exception {
        final Path projectDir = newProjectDir("latest-acme-platform");
        createProject(projectDir, List.of("rabbit", "quarkus-rest", "quarkus-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom(), ArtifactCoords.pom(ACME_PLATFORM_KEY, "acme-zoo-bom", "5.0.5")),
                List.of(ArtifactCoords.jar("org.acme", "acme-rabbit", null),
                        ArtifactCoords.jar(OTHER_PLATFORM_KEY, "quarkus-rest", null),
                        ArtifactCoords.jar(OTHER_PLATFORM_KEY, "quarkus-magic", null)),
                "3.0.5");
    }
}
