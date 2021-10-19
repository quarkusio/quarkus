package io.quarkus.registry;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformCatalogImpl;
import io.quarkus.registry.catalog.PlatformImpl;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformReleaseImpl;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.catalog.PlatformStreamImpl;
import io.quarkus.registry.catalog.json.JsonCatalogMerger;
import io.quarkus.registry.catalog.json.JsonPlatformReleaseVersion;
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
        catalogs.add(PlatformCatalogImpl.builder()
                .addPlatform(PlatformImpl.builder()
                        .withPlatformKey("platform")
                        .addStream(PlatformStreamImpl.builder()
                                .withId("2.0")
                                .addRelease(PlatformReleaseImpl.builder()
                                        .withQuarkusCoreVersion("2.0.0")
                                        .withVersion(PlatformReleaseVersion.fromString("2.2.2"))
                                        .withMemberBoms(Collections.singletonList(
                                                ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:2.2.2")))
                                        .build())
                                .build())
                        .addStream(PlatformStreamImpl.builder()
                                .withId("1.0")
                                .addRelease(PlatformReleaseImpl.builder()
                                        .withQuarkusCoreVersion("1.0.1")
                                        .withVersion(PlatformReleaseVersion.fromString("1.1.2"))
                                        .withMemberBoms(Collections.singletonList(
                                                ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:1.1.2")))
                                        .build())
                                .build())
                        .build())
                .build());

        catalogs.add(PlatformCatalogImpl.builder()
                .addPlatform(PlatformImpl.builder()
                        .withPlatformKey("platform")
                        .addStream(PlatformStreamImpl.builder()
                                .withId("2.0")
                                .addRelease(PlatformReleaseImpl.builder()
                                        .withQuarkusCoreVersion("2.0.1")
                                        .withVersion(PlatformReleaseVersion.fromString("2.2.3"))
                                        .withMemberBoms(Collections.singletonList(
                                                ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:2.2.3")))
                                        .build())
                                .build())
                        .addStream(PlatformStreamImpl.builder()
                                .withId("1.0")
                                .addRelease(PlatformReleaseImpl.builder()
                                        .withQuarkusCoreVersion("1.0.0")
                                        .withVersion(PlatformReleaseVersion.fromString("1.1.1"))
                                        .withMemberBoms(Collections.singletonList(
                                                ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:1.1.1")))
                                        .build())
                                .build())
                        .build())
                .build());

        final PlatformCatalog merged = JsonCatalogMerger.mergePlatformCatalogs(catalogs);

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
        assertThat(release.getVersion()).isEqualTo(JsonPlatformReleaseVersion.fromString("2.2.2"));
        assertThat(release.getQuarkusCoreVersion()).isEqualTo("2.0.0");
        release = releases.next();
        assertThat(release.getVersion()).isEqualTo(JsonPlatformReleaseVersion.fromString("2.2.3"));
        assertThat(release.getQuarkusCoreVersion()).isEqualTo("2.0.1");

        stream = streams.next();
        assertThat(stream.getId()).isEqualTo("1.0");
        assertThat(stream.getReleases().size()).isEqualTo(2);
        releases = stream.getReleases().iterator();
        release = releases.next();
        assertThat(release.getVersion()).isEqualTo(JsonPlatformReleaseVersion.fromString("1.1.2"));
        assertThat(release.getQuarkusCoreVersion()).isEqualTo("1.0.1");
        release = releases.next();
        assertThat(release.getVersion()).isEqualTo(JsonPlatformReleaseVersion.fromString("1.1.1"));
        assertThat(release.getQuarkusCoreVersion()).isEqualTo("1.0.0");
    }
}
