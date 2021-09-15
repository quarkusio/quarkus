package io.quarkus.bootstrap.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GACTV;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class PlatformImportsTest {

    private final List<PlatformProps> platformProps = new ArrayList<>();

    @AfterEach
    public void cleanUp() {
        for (PlatformProps p : platformProps) {
            p.delete();
        }
    }

    @Test
    public void singlePlatformReleaseInfo() throws Exception {
        final PlatformProps props = newPlatformProps();
        props.setProperty("platform.quarkus.native.builder-image", "url");
        props.setRelease(new PlatformReleaseInfo("io.playground", "1.1", "1",
                "io.playground:playground-bom::pom:1.1.1,io.playground:acme-bom::pom:2.2.2,io.playground:foo-bom::pom:3.3.3"));

        final PlatformImportsImpl pi = new PlatformImportsImpl();
        props.importRelease(pi);

        final PlatformInfo platform = pi.getPlatform("io.playground");
        assertNotNull(platform);
        assertEquals("io.playground", platform.getPlatformKey());
        final PlatformStreamInfo stream = platform.getStream("1.1");
        assertNotNull(stream);
        final PlatformReleaseInfo release = stream.getRelease("1");
        assertEquals("io.playground", release.getPlatformKey());
        assertEquals("1.1", release.getStream());
        assertEquals("1", release.getVersion());
        final List<ArtifactCoords> boms = release.getBoms();
        assertEquals(Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.1"),
                GACTV.fromString("io.playground:acme-bom::pom:2.2.2"),
                GACTV.fromString("io.playground:foo-bom::pom:3.3.3")), boms);
        assertEquals(1, stream.getReleases().size());
        assertEquals(1, platform.getStreams().size());
        assertEquals(1, pi.getPlatforms().size());

        assertTrue(pi.isAligned(Collections.singletonMap("io.playground",
                Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.1"),
                        GACTV.fromString("io.playground:acme-bom::pom:2.2.2")))));
        assertFalse(pi.isAligned(Collections.singletonMap("io.playground",
                Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.2"),
                        GACTV.fromString("io.playground:acme-bom::pom:2.2.2")))));
    }

    @Test
    public void multiplePlatformReleaseInTheSameStream() throws Exception {
        final PlatformProps member1 = newPlatformProps();
        member1.setProperty("platform.quarkus.native.builder-image", "url");
        member1.setRelease(new PlatformReleaseInfo("io.playground", "1.1", "1",
                "io.playground:playground-bom::pom:1.1.1,io.playground:acme-bom::pom:2.2.2,io.playground:foo-bom::pom:3.3.3"));

        final PlatformProps member2 = newPlatformProps();
        member2.setProperty("platform.quarkus.native.builder-image", "url");
        member2.setRelease(new PlatformReleaseInfo("io.playground", "1.1", "2",
                "io.playground:playground-bom::pom:1.1.2,io.playground:acme-bom::pom:2.2.3,io.playground:foo-bom::pom:3.3.3"));

        final PlatformImportsImpl pi = new PlatformImportsImpl();
        member1.importRelease(pi);
        member2.importRelease(pi);

        final PlatformInfo platform = pi.getPlatform("io.playground");
        assertNotNull(platform);
        assertEquals("io.playground", platform.getPlatformKey());
        final PlatformStreamInfo stream = platform.getStream("1.1");
        assertNotNull(stream);
        final PlatformReleaseInfo release = stream.getRelease("1");
        assertEquals("io.playground", release.getPlatformKey());
        assertEquals("1.1", release.getStream());
        assertEquals("1", release.getVersion());
        final List<ArtifactCoords> boms = release.getBoms();
        assertEquals(Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.1"),
                GACTV.fromString("io.playground:acme-bom::pom:2.2.2"),
                GACTV.fromString("io.playground:foo-bom::pom:3.3.3")), boms);
        assertEquals(2, stream.getReleases().size());
        assertEquals(1, platform.getStreams().size());
        assertEquals(1, pi.getPlatforms().size());

        assertTrue(pi.isAligned(Collections.singletonMap("io.playground",
                Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.1"),
                        GACTV.fromString("io.playground:acme-bom::pom:2.2.2")))));
        assertTrue(pi.isAligned(Collections.singletonMap("io.playground",
                Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.2"),
                        GACTV.fromString("io.playground:acme-bom::pom:2.2.3")))));
        assertFalse(pi.isAligned(Collections.singletonMap("io.playground",
                Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.2"),
                        GACTV.fromString("io.playground:acme-bom::pom:2.2.2")))));
    }

    private PlatformProps newPlatformProps() throws IOException {
        final PlatformProps p = new PlatformProps();
        platformProps.add(p);
        return p;
    }

    private static class PlatformProps {

        private final Path path;
        private Properties props = new Properties();

        private PlatformProps() throws IOException {
            path = Files.createTempFile("quarkus", "platform-imports");
        }

        private void setRelease(PlatformReleaseInfo release) {
            props.setProperty(release.getPropertyName(), release.getPropertyValue());
        }

        private void setProperty(String name, String value) {
            props.setProperty(name, value);
        }

        private void importRelease(PlatformImportsImpl pi) throws IOException {
            try (BufferedWriter w = Files.newBufferedWriter(path)) {
                props.store(w, "test playground platform props");
            }
            props = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                props.load(reader);
            }
            for (Map.Entry<?, ?> prop : props.entrySet()) {
                if (PlatformImportsImpl.isPlatformReleaseInfo(prop.getKey().toString())) {
                    pi.addPlatformRelease(prop.getKey().toString(), prop.getValue().toString());
                }
            }
        }

        private void delete() {
            IoUtils.recursiveDelete(path);
        }
    }
}
