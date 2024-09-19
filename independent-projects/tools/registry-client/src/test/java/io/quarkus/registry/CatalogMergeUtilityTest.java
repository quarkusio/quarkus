package io.quarkus.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import io.quarkus.registry.catalog.PlatformStream;

/**
 * Verify package-private functionality to merge extension catalogs
 */
public class CatalogMergeUtilityTest {
    @Test
    void testMergePlatformCatalogs() {
        final List<PlatformCatalog> catalogs = new ArrayList<>();
        catalogs.add(PlatformCatalog.builder()
                .addPlatform(Platform.builder()
                        .setPlatformKey("platform")
                        .addStream(PlatformStream.builder()
                                .setId("2.0")
                                .addRelease(PlatformRelease.builder()
                                        .setQuarkusCoreVersion("2.0.0")
                                        .setVersion(PlatformReleaseVersion.fromString("2.2.2"))
                                        .setMemberBoms(Collections.singletonList(
                                                ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:2.2.2")))
                                        .build())
                                .build())
                        .addStream(PlatformStream.builder()
                                .setId("1.0")
                                .addRelease(PlatformRelease.builder()
                                        .setQuarkusCoreVersion("1.0.1")
                                        .setVersion(PlatformReleaseVersion.fromString("1.1.2"))
                                        .setMemberBoms(Collections.singletonList(
                                                ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:1.1.2")))
                                        .build())
                                .build())
                        .build())
                .build());

        catalogs.add(PlatformCatalog.builder()
                .addPlatform(Platform.builder()
                        .setPlatformKey("platform")
                        .addStream(PlatformStream.builder()
                                .setId("2.0")
                                .addRelease(PlatformRelease.builder()
                                        .setQuarkusCoreVersion("2.0.1")
                                        .setVersion(PlatformReleaseVersion.fromString("2.2.3"))
                                        .setMemberBoms(Collections.singletonList(
                                                ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:2.2.3")))
                                        .build())
                                .build())
                        .addStream(PlatformStream.builder()
                                .setId("1.0")
                                .addRelease(PlatformRelease.builder()
                                        .setQuarkusCoreVersion("1.0.0")
                                        .setVersion(PlatformReleaseVersion.fromString("1.1.1"))
                                        .setMemberBoms(Collections.singletonList(
                                                ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:1.1.1")))
                                        .build())
                                .build())
                        .build())
                .build());

        final PlatformCatalog merged = CatalogMergeUtility.mergePlatformCatalogs(catalogs);

        Collection<Platform> platforms = merged.getPlatforms();
        assertThat(platforms.size()).isEqualTo(1);

        Platform platform = platforms.iterator().next();
        assertThat(platform.getPlatformKey()).isEqualTo("platform");
        assertThat(platform.getStreams().size()).isEqualTo(2);

        Iterator<PlatformStream> streams = platform.getStreams().iterator();
        PlatformStream stream = streams.next();
        assertThat(stream.getId()).isEqualTo("2.0");
        assertThat(stream.getReleases().size()).isEqualTo(2);
        Iterator<PlatformRelease> releases = stream.getReleases().iterator();
        PlatformRelease release = releases.next();
        assertThat(release.getVersion()).isEqualTo(PlatformReleaseVersion.fromString("2.2.2"));
        assertThat(release.getQuarkusCoreVersion()).isEqualTo("2.0.0");
        release = releases.next();
        assertThat(release.getVersion()).isEqualTo(PlatformReleaseVersion.fromString("2.2.3"));
        assertThat(release.getQuarkusCoreVersion()).isEqualTo("2.0.1");

        stream = streams.next();
        assertThat(stream.getId()).isEqualTo("1.0");
        assertThat(stream.getReleases().size()).isEqualTo(2);
        releases = stream.getReleases().iterator();
        release = releases.next();
        assertThat(release.getVersion()).isEqualTo(PlatformReleaseVersion.fromString("1.1.2"));
        assertThat(release.getQuarkusCoreVersion()).isEqualTo("1.0.1");
        release = releases.next();
        assertThat(release.getVersion()).isEqualTo(PlatformReleaseVersion.fromString("1.1.1"));
        assertThat(release.getQuarkusCoreVersion()).isEqualTo("1.0.0");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testMergeProjectCodestartData() {

        var first = ExtensionCatalog.builder()
                .setMetadata("project",
                        Map.of("codestart-data",
                                Map.of("acme-codestart",
                                        Map.of("a", "1",
                                                "b", "1",
                                                "nested",
                                                Map.of("c", "1",
                                                        "d", "1")))))
                .build();

        var second = ExtensionCatalog.builder()
                .setMetadata("project",
                        Map.of("codestart-data",
                                Map.of("acme-codestart",
                                        Map.of("z", "2",
                                                "b", "2",
                                                "nested",
                                                Map.of("y", "2",
                                                        "d", "2")))))
                .build();

        var result = CatalogMergeUtility.merge(List.of(first, second));
        assertThat(result).isNotNull();

        var map = (Map) result.getMetadata().get("project");
        assertThat(map).isNotNull();
        map = (Map) map.get("codestart-data");
        assertThat(map).isNotNull();
        map = (Map) map.get("acme-codestart");
        assertThat(map).isNotNull();

        assertThat(map).hasFieldOrPropertyWithValue("a", "1");
        assertThat(map).hasFieldOrPropertyWithValue("b", "1");
        assertThat(map).hasFieldOrPropertyWithValue("z", "2");

        map = (Map) map.get("nested");
        assertThat(map).hasFieldOrPropertyWithValue("c", "1");
        assertThat(map).hasFieldOrPropertyWithValue("d", "1");
        assertThat(map).hasFieldOrPropertyWithValue("y", "2");
    }
}
