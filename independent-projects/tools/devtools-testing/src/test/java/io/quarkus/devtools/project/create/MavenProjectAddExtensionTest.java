package io.quarkus.devtools.project.create;

import static io.quarkus.devtools.project.create.MultiplePlatformBomsTestBase.enableRegistryClient;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.registry.catalog.PlatformStreamCoords;

public class MavenProjectAddExtensionTest extends MultiplePlatformBomsTestBase {

    private static final String PLATFORM_KEY = "io.test.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                .baseDir(configDir())
                .newRegistry("registry.test.io")
                .newPlatform(PLATFORM_KEY)
                .newStream("1.0")
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
    public void test() throws Exception {
        final Path projectDir = newProjectDir("existing-project");
        createProject(projectDir, new PlatformStreamCoords(null, "0.5"), List.of("ext-a"));

        // valid characters, existing dependency
        QuarkusCommandOutcome outcome = addExtensions(projectDir, List.of("io.quarkus:quarkus-info:3.20.1"));
        assertTrue(outcome.isSuccess());

        // invalid characters (tilde in artifactId)
        outcome = addExtensions(projectDir,
                List.of("io.quarkiverse.businessscore:quarkus-business-score-health~:1.0.0.Alpha4"));
        assertFalse(outcome.isSuccess());

        // valid characters, questionable dependency
        outcome = addExtensions(projectDir, List.of("group:artifact:version"));
        assertTrue(outcome.isSuccess());
    }
}
