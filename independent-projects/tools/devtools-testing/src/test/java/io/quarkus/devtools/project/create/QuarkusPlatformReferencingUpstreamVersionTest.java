package io.quarkus.devtools.project.create;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.ArtifactCoords;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class QuarkusPlatformReferencingUpstreamVersionTest extends MultiplePlatformBomsTestBase {

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
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.4 release
                .newRelease("2.0.4-downstream")
                .quarkusVersion("2.2.2-downstream")
                .upstreamQuarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                // foo platform member
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "2.0.4-downstream").release()
                .stream().platform().registry()
                .newNonPlatformCatalog("2.2.2-downstream").addExtension("io.acme", "ext-d", "6.0-downstream").registry()
                .clientBuilder()
                .newRegistry("upstream.registry.test")
                // platform key
                .newPlatform(UPSTREAM_PLATFORM_KEY)
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.4 release
                .newRelease("2.0.4")
                .quarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                .newMember("acme-foo-bom").addExtension("io.acme", "ext-a", "2.0.4").release()
                .newMember("acme-bar-bom").addExtension("io.acme", "ext-b", "2.0.4").release()
                .stream().platform().registry()
                .newNonPlatformCatalog("2.2.2").addExtension("io.acme", "ext-c", "5.1").addExtension("io.acme", "ext-d", "6.0")
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();
    }

    protected String getMainPlatformKey() {
        return DOWNSTREAM_PLATFORM_KEY;
    }

    @Test
    public void addExtensionsFromAlreadyImportedPlatform() throws Exception {
        final Path projectDir = newProjectDir("downstream-upstream-platform");
        createProject(projectDir, Arrays.asList("ext-a"));

        assertModel(projectDir,
                toPlatformBomCoords("acme-foo-bom"),
                Arrays.asList(new ArtifactCoords("io.acme", "ext-a", null)),
                "2.0.4-downstream");

        addExtensions(projectDir, Arrays.asList("ext-b", "ext-c", "ext-d"));
        assertModel(projectDir,
                Arrays.asList(mainPlatformBom(), platformMemberBomCoords("acme-foo-bom"),
                        new ArtifactCoords(UPSTREAM_PLATFORM_KEY, "acme-bar-bom", "pom", "2.0.4")),
                Arrays.asList(new ArtifactCoords("io.acme", "ext-a", null),
                        new ArtifactCoords("io.acme", "ext-b", null),
                        new ArtifactCoords("io.acme", "ext-c", "jar", "5.1"),
                        new ArtifactCoords("io.acme", "ext-d", "jar", "6.0-downstream")),
                "2.0.4-downstream");
    }
}
