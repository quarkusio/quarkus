package io.quarkus.bootstrap.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.maven.dependency.GACTV;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class PlatformStreamInfoTest {

    @Test
    public void emptyIsAligned() throws Exception {
        assertTrue(new PlatformStreamInfo("1.1")
                .isAligned(Collections.singletonList(GACTV.fromString("io.playground:playground-bom::pom:1.1.1"))));
    }

    @Test
    public void isAligned() throws Exception {
        final PlatformStreamInfo stream = new PlatformStreamInfo("1.1");
        stream.addIfNotPresent("1", () -> new PlatformReleaseInfo("io.playground", "playground-bom", "1.1",
                "io.playground:playground-bom::pom:1.1.1,org.acme:acme-bom::pom:2.2.2,com.foo:foo-bom::pom:3.3.3"));
        stream.addIfNotPresent("2", () -> new PlatformReleaseInfo("io.playground", "playground-bom", "1.1",
                "io.playground:playground-bom::pom:1.1.2,org.acme:acme-bom::pom:2.2.3,com.foo:foo-bom::pom:3.3.3"));
        assertTrue(stream.isAligned(Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.1"),
                GACTV.fromString("org.acme:acme-bom::pom:2.2.2"))));
        assertTrue(stream.isAligned(Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.2"),
                GACTV.fromString("org.acme:acme-bom::pom:2.2.3"))));
        assertFalse(stream.isAligned(Arrays.asList(GACTV.fromString("io.playground:playground-bom::pom:1.1.2"),
                GACTV.fromString("org.acme:acme-bom::pom:2.2.2"))));
    }
}
