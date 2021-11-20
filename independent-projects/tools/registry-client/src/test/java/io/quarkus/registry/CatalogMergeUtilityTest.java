package io.quarkus.registry;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import io.quarkus.registry.catalog.PlatformStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verify package-private functionality to merge extension catalogs
 */
public class CatalogMergeUtilityTest {
    @Test
    void testMergePlatformCatalogs() throws Exception {
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
}
