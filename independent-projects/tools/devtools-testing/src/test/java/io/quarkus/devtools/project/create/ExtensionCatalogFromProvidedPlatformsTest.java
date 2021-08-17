package io.quarkus.devtools.project.create;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ExtensionCatalogFromProvidedPlatformsTest extends MultiplePlatformBomsTestBase {

    private static final String MAIN_PLATFORM_KEY = "org.acme.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("registry.acme.org")
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
                .newMember("acme-foo-bom").addExtension("acme-foo").release()
                .stream().platform()
                // 1.0 STREAM
                .newStream("1.0")
                // 1.0.1 release
                .newRelease("1.0.1")
                .quarkusVersion("1.1.2")
                .addCoreMember()
                .newMember("acme-foo-bom").addExtension("acme-foo").release()
                .newMember("acme-baz-bom").addExtension("acme-baz").release()
                .registry()
                .newNonPlatformCatalog("1.1.2")
                .addExtension("org.acme", "acme-quarkus-other", "5.5.5")
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();

        // install 1.0.0 version which is not recommended by the registry any more
        installNotRecommendedVersion("1.0.1", "1.0.0", "acme-foo-bom");
        installNotRecommendedVersion("1.0.1", "1.0.0", "acme-baz-bom");
        installNotRecommendedVersion("1.0.1", "1.0.0", "quarkus-bom");
    }

    private static void installNotRecommendedVersion(String baseRecommendedVersion, String nonRecommendedVersion,
            String memberArtifactId) throws IOException {
        Path repoDir = TestRegistryClientBuilder.getMavenRepoDir(configDir());
        Path p = repoDir;
        for (String s : MAIN_PLATFORM_KEY.split("\\.")) {
            p = p.resolve(s);
        }
        final Path groupIdDir = p;

        p = groupIdDir.resolve(memberArtifactId).resolve(baseRecommendedVersion)
                .resolve(memberArtifactId + "-" + baseRecommendedVersion + ".pom");
        final Model model = ModelUtils.readModel(p);
        model.setVersion(nonRecommendedVersion);
        for (Dependency d : model.getDependencyManagement().getDependencies()) {
            if (baseRecommendedVersion.equals(d.getVersion())) {
                d.setVersion(nonRecommendedVersion);
            }
        }
        p = p.getParent().getParent().resolve(nonRecommendedVersion)
                .resolve(memberArtifactId + "-" + nonRecommendedVersion + ".pom");
        Files.createDirectories(p.getParent());
        ModelUtils.persistModel(p, model);

        p = TestRegistryClientBuilder.getRegistryMemberCatalogPath(
                TestRegistryClientBuilder.getRegistryDir(configDir(), "registry.acme.org"),
                new ArtifactCoords(MAIN_PLATFORM_KEY, memberArtifactId, null, "pom", baseRecommendedVersion));
        final String jsonContent = Files.readString(p).replace(baseRecommendedVersion, nonRecommendedVersion);
        String jsonName = p.getFileName().toString().replace(baseRecommendedVersion, nonRecommendedVersion);
        Files.writeString(p.getParent().resolve(jsonName), jsonContent);
    }

    protected String getMainPlatformKey() {
        return MAIN_PLATFORM_KEY;
    }

    @Test
    public void test() throws Exception {
        final ExtensionCatalog catalog = ExtensionCatalogResolver.builder().build().resolveExtensionCatalog(
                Collections.singletonList(ArtifactCoords.fromString(MAIN_PLATFORM_KEY + ":acme-baz-bom::pom:1.0.0")));
        assertThat(Arrays.asList(new ArtifactCoords[] {
                ArtifactCoords.fromString("io.quarkus:quarkus-core:1.1.2"),
                ArtifactCoords.fromString(MAIN_PLATFORM_KEY + ":acme-foo:1.0.0"),
                ArtifactCoords.fromString(MAIN_PLATFORM_KEY + ":acme-baz:1.0.0"),
                ArtifactCoords.fromString("org.acme:acme-quarkus-other:5.5.5")
        })).isEqualTo(
                catalog.getExtensions().stream().map(e -> e.getArtifact()).collect(Collectors.toList()));
    }
}
