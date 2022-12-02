package io.quarkus.maven;

import static io.quarkus.maven.ExtensionDescriptorMojo.getCodestartArtifact;
import static io.quarkus.maven.ExtensionDescriptorMojo.getSourceRepo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

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

    @Test
    void testGetSourceControlCoordinates() {
        // From maven this property should be set, running in an IDE it won't be unless specially configured
        if (System.getenv("GITHUB_REPOSITORY") != null) {
            Map repo = getSourceRepo();
            assertNotNull(repo);
            assertTrue(repo.get("url").toString().matches("https://github.com/some/repo"));
        } else {
            assertNull(getSourceRepo());
        }
    }
}
