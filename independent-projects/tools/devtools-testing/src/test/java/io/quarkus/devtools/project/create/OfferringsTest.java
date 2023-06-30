package io.quarkus.devtools.project.create;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.PlatformStreamCoords;

public class OfferringsTest extends MultiplePlatformBomsTestBase {

    private static final String MAIN_PLATFORM_KEY = "org.acme.platform";

    @BeforeAll
    public static void setup() throws Exception {
        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry
                .newRegistry("registry.acme.org")
                .enableOfferings("core", "foo")
                // platform key
                .newPlatform(MAIN_PLATFORM_KEY)
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.4 release
                .newRelease("2.0.4")
                .quarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember().setOfferings("core").release()
                // foo platform member
                .newMember("acme-foo-bom").setOfferings("foo").addExtension("acme-foo").release()
                .newMember("acme-baz-bom").setOfferings("baz").addExtension("acme-baz").release()
                .stream().platform()
                // 1.0 STREAM
                .newStream("1.0")
                // 1.0.1 release
                .newRelease("1.0.1")
                .quarkusVersion("1.1.2")
                .addCoreMember().setOfferings("core").release()
                .newMember("acme-foo-bom").setOfferings("foo").addExtension("acme-foo").release()
                .newMember("acme-baz-bom").setOfferings("baz", "core").addExtension("acme-baz").release()
                .registry()
                .newNonPlatformCatalog("1.1.2")
                .addExtension("org.acme", "acme-quarkus-other", "5.5.5")
                .registry()
                .clientBuilder()
                .build();

        enableRegistryClient();

    }

    protected String getMainPlatformKey() {
        return MAIN_PLATFORM_KEY;
    }

    @Test
    public void allEnabled() throws Exception {
        var catalog = ExtensionCatalogResolver.builder().build().resolveExtensionCatalog();
        assertThat(catalog.getExtensions()).hasSize(4);
        final Map<ArtifactKey, Set<ArtifactCoords>> extensionMap = toExtensionMap(catalog);
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("io.quarkus:quarkus-core"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "quarkus-bom", "2.0.4"),
                        ArtifactCoords.pom("org.acme.platform", "quarkus-bom", "1.0.1")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme.platform:acme-foo"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "acme-foo-bom", "2.0.4"),
                        ArtifactCoords.pom("org.acme.platform", "acme-foo-bom", "1.0.1")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme.platform:acme-baz"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "acme-baz-bom", "1.0.1")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme:acme-quarkus-other"),
                Set.of(ArtifactCoords.pom("io.quarkus", "quarkus-bom", "1.1.2")));
    }

    @Test
    public void stream20() throws Exception {
        var catalog = ExtensionCatalogResolver.builder().build()
                .resolveExtensionCatalog(PlatformStreamCoords.fromString("2.0"));
        assertThat(catalog.getExtensions()).hasSize(2);
        final Map<ArtifactKey, Set<ArtifactCoords>> extensionMap = toExtensionMap(catalog);
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("io.quarkus:quarkus-core"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "quarkus-bom", "2.0.4")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme.platform:acme-foo"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "acme-foo-bom", "2.0.4")));
    }

    @Test
    public void quarkusVersion222() throws Exception {
        var catalog = ExtensionCatalogResolver.builder().build().resolveExtensionCatalog("2.2.2");
        assertThat(catalog.getExtensions()).hasSize(2);
        final Map<ArtifactKey, Set<ArtifactCoords>> extensionMap = toExtensionMap(catalog);
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("io.quarkus:quarkus-core"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "quarkus-bom", "2.0.4")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme.platform:acme-foo"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "acme-foo-bom", "2.0.4")));
    }

    @Test
    public void stream10() throws Exception {
        var catalog = ExtensionCatalogResolver.builder().build()
                .resolveExtensionCatalog(PlatformStreamCoords.fromString("1.0"));
        assertThat(catalog.getExtensions()).hasSize(4);
        final Map<ArtifactKey, Set<ArtifactCoords>> extensionMap = toExtensionMap(catalog);
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("io.quarkus:quarkus-core"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "quarkus-bom", "1.0.1")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme.platform:acme-foo"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "acme-foo-bom", "1.0.1")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme.platform:acme-baz"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "acme-baz-bom", "1.0.1")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme:acme-quarkus-other"),
                Set.of(ArtifactCoords.pom("io.quarkus", "quarkus-bom", "1.1.2")));
    }

    @Test
    public void quarkusVersion112() throws Exception {
        var catalog = ExtensionCatalogResolver.builder().build().resolveExtensionCatalog("1.1.2");
        assertThat(catalog.getExtensions()).hasSize(4);
        final Map<ArtifactKey, Set<ArtifactCoords>> extensionMap = toExtensionMap(catalog);
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("io.quarkus:quarkus-core"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "quarkus-bom", "1.0.1")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme.platform:acme-foo"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "acme-foo-bom", "1.0.1")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme.platform:acme-baz"),
                Set.of(ArtifactCoords.pom("org.acme.platform", "acme-baz-bom", "1.0.1")));
        assertThat(extensionMap).containsEntry(
                ArtifactKey.fromString("org.acme:acme-quarkus-other"),
                Set.of(ArtifactCoords.pom("io.quarkus", "quarkus-bom", "1.1.2")));
    }

    private static Map<ArtifactKey, Set<ArtifactCoords>> toExtensionMap(ExtensionCatalog catalog) {
        final Map<ArtifactKey, Set<ArtifactCoords>> extensionMap = new HashMap<>(catalog.getExtensions().size());
        for (Extension e : catalog.getExtensions()) {
            final Set<ArtifactCoords> boms = new HashSet<>(e.getOrigins().size());
            for (ExtensionOrigin o : e.getOrigins()) {
                boms.add(o.getBom());
            }
            extensionMap.put(e.getArtifact().getKey(), boms);
        }
        return extensionMap;
    }
}
