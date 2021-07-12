package io.quarkus.devtools.project.create;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.ArtifactCoords;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MavenProjectImportingMultipleBomsFromMultiplePlatformsTest extends MultiplePlatformBomsTestBase {

    private static final String MAIN_PLATFORM_KEY = "io.other.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("multiplatform.registry.test")
                // platform key
                .newPlatform(MAIN_PLATFORM_KEY)
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.4 release
                .newRelease("2.0.4")
                .quarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                // foo platform member
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "2.0.4").addExtension("ext-b").release()
                .stream().platform().registry()
                .newPlatform("io.other.platform")
                // 1.0 STREAM
                .newStream("1.0")
                // 1.0.1 release
                .newRelease("1.0.1")
                .quarkusVersion("1.1.2")
                .addCoreMember()
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "1.0.1").release()
                .newMember("acme-baz-bom").addExtension("io.acme", "ext-b", "1.0.1").release()
                .stream().platform().registry()
                .newNonPlatformCatalog("2.2.2").addExtension("io.acme", "ext-c", "5.1").registry()
                .newNonPlatformCatalog("1.1.2").addExtension("io.acme", "ext-c", "5.0").registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return "io.other.platform";
    }

    @Test
    public void addExtensionsFromAlreadyImportedPlatform() throws Exception {
        final Path projectDir = newProjectDir("non-default-base-platform");
        createProject(projectDir, "1.1.2", Arrays.asList("ext-a"));

        assertModel(projectDir,
                toPlatformBomCoords("acme-foo-bom"),
                Collections.singletonList(new ArtifactCoords("io.acme", "ext-a", "jar", null)),
                "1.0.1");

        addExtensions(projectDir, Arrays.asList("ext-b", "ext-c"));
        assertModel(projectDir,
                toPlatformBomCoords("acme-foo-bom", "acme-baz-bom"),
                Arrays.asList(new ArtifactCoords("io.acme", "ext-a", "jar", null),
                        new ArtifactCoords("io.acme", "ext-b", "jar", null),
                        new ArtifactCoords("io.acme", "ext-c", "jar", "5.0")),
                "1.0.1");
    }
}
