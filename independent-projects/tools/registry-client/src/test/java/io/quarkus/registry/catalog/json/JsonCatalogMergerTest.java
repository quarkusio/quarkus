package io.quarkus.registry.catalog.json;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JsonCatalogMergerTest {

    @Test
    void testMergePlatformCatalogs() throws Exception {

        final List<PlatformCatalog> catalogs = new ArrayList<>();

        JsonPlatformCatalog c = new JsonPlatformCatalog();
        catalogs.add(c);
        JsonPlatform p = new JsonPlatform();
        c.addPlatform(p);
        p.setPlatformKey("platform");
        JsonPlatformStream s = new JsonPlatformStream();
        s.setId("2.0");
        p.addStream(s);
        JsonPlatformRelease r = new JsonPlatformRelease();
        r.setQuarkusCoreVersion("2.0.0");
        r.setVersion(JsonPlatformReleaseVersion.fromString("2.2.2"));
        r.setMemberBoms(Collections.singletonList(ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:2.2.2")));
        s.addRelease(r);

        s = new JsonPlatformStream();
        s.setId("1.0");
        p.addStream(s);
        r = new JsonPlatformRelease();
        r.setQuarkusCoreVersion("1.0.1");
        r.setVersion(JsonPlatformReleaseVersion.fromString("1.1.2"));
        r.setMemberBoms(Collections.singletonList(ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:1.1.2")));
        s.addRelease(r);

        c = new JsonPlatformCatalog();
        catalogs.add(c);
        p = new JsonPlatform();
        c.addPlatform(p);
        p.setPlatformKey("platform");
        s = new JsonPlatformStream();
        s.setId("2.0");
        p.addStream(s);
        r = new JsonPlatformRelease();
        r.setQuarkusCoreVersion("2.0.1");
        r.setVersion(JsonPlatformReleaseVersion.fromString("2.2.3"));
        r.setMemberBoms(Collections.singletonList(ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:2.2.3")));
        s.addRelease(r);

        s = new JsonPlatformStream();
        s.setId("1.0");
        p.addStream(s);
        r = new JsonPlatformRelease();
        r.setQuarkusCoreVersion("1.0.0");
        r.setVersion(JsonPlatformReleaseVersion.fromString("1.1.1"));
        r.setMemberBoms(Collections.singletonList(ArtifactCoords.fromString("org.acme:acme-quarkus-bom::pom:1.1.1")));
        s.addRelease(r);

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
