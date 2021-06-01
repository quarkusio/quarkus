package io.quarkus.bootstrap.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class PlatformInfoTest {

    @Test
    public void emptyIsAligned() throws Exception {
        assertTrue(new PlatformInfo("p")
                .isAligned(Collections.singletonList(AppArtifactCoords.fromString("io.playground:playground-bom::pom:1.1.1"))));
    }

    @Test
    public void singleStreamIsAligned() throws Exception {
        final PlatformInfo platform = new PlatformInfo("p");
        final PlatformStreamInfo stream = platform.getOrCreateStream("1.1");
        stream.addIfNotPresent("1", () -> new PlatformReleaseInfo("io.playground", "playground-bom", "1.1",
                "io.playground:playground-bom::pom:1.1.1,org.acme:acme-bom::pom:2.2.2,com.foo:foo-bom::pom:3.3.3"));
        stream.addIfNotPresent("2", () -> new PlatformReleaseInfo("io.playground", "playground-bom", "1.1",
                "io.playground:playground-bom::pom:1.1.2,org.acme:acme-bom::pom:2.2.3,com.foo:foo-bom::pom:3.3.3"));

        assertTrue(platform.isAligned(Arrays.asList(AppArtifactCoords.fromString("io.playground:playground-bom::pom:1.1.1"),
                AppArtifactCoords.fromString("org.acme:acme-bom::pom:2.2.2"))));
        assertTrue(platform.isAligned(Arrays.asList(AppArtifactCoords.fromString("io.playground:playground-bom::pom:1.1.2"),
                AppArtifactCoords.fromString("org.acme:acme-bom::pom:2.2.3"))));
        assertFalse(platform.isAligned(Arrays.asList(AppArtifactCoords.fromString("io.playground:playground-bom::pom:1.1.2"),
                AppArtifactCoords.fromString("org.acme:acme-bom::pom:2.2.2"))));
    }

    @Test
    public void multipleStreamsAreNotAligned() throws Exception {
        final PlatformInfo platform = new PlatformInfo("p");
        platform.getOrCreateStream("1.1");
        platform.getOrCreateStream("1.2");
        assertFalse(platform
                .isAligned(Collections.singletonList(AppArtifactCoords.fromString("io.playground:playground-bom::pom:1.1.1"))));
    }
}
