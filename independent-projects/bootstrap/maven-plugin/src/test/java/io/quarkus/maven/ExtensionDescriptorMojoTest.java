package io.quarkus.maven;

import static io.quarkus.maven.ExtensionDescriptorMojo.getCodestartArtifact;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExtensionDescriptorMojoTest {

    @Test
    void testGetCodestartArtifact() {
        assertEquals("io.quarkus:my-ext:999-SN",
                getCodestartArtifact("io.quarkus:my-ext", "999-SN"));
        assertEquals("io.quarkus:my-ext:codestarts:jar:999-SN",
                getCodestartArtifact("io.quarkus:my-ext:codestarts:jar:${project.version}", "999-SN"));
        assertEquals("io.quarkus:my-ext:codestarts:jar:1.0",
                getCodestartArtifact("io.quarkus:my-ext:codestarts:jar:1.0", "999-SN"));
        assertEquals("io.quarkus:my-ext:999-SN",
                getCodestartArtifact("io.quarkus:my-ext:${project.version}", "999-SN"));
    }
}
