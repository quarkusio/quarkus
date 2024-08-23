package io.quarkus.maven.dependency;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArtifactCoordsPatternTest {

    @Test
    public void groupId() {
        final ArtifactCoordsPattern p = ArtifactCoordsPattern.of("org.my-group");
        Assertions.assertTrue(p.matches("org.my-group", "foo", "", "jar", "1.2.3"));
        Assertions.assertTrue(p.matches("org.my-group", "foo", null, "jar", "1.2.3"));
        Assertions.assertFalse(p.matches("org.foo", "foo", null, "jar", "1.2.3"));
    }

    @Test
    public void groupIdPrefix() {
        final ArtifactCoordsPattern p = ArtifactCoordsPattern.of("org.my-group*");
        Assertions.assertTrue(p.matches("org.my-group", "foo", null, "jar", "1.2.3"));
        Assertions.assertTrue(p.matches("org.my-group-bar", "foo", null, "jar", "1.2.3"));
        Assertions.assertFalse(p.matches("org.foo", "foo", null, "jar", "1.2.3"));
    }

    @Test
    public void artifactId() {
        final ArtifactCoordsPattern p = ArtifactCoordsPattern.of("org.my-group:my-artifact");
        Assertions.assertTrue(p.matches("org.my-group", "my-artifact", null, "jar", "1.2.3"));
        Assertions.assertTrue(p.matches("org.my-group", "my-artifact", null, "jar", "1.2.4"));
        Assertions.assertFalse(p.matches("org.my-group", "foo", null, "jar", "1.2.3"));
    }

    @Test
    public void gav() {
        final ArtifactCoordsPattern p = ArtifactCoordsPattern.of("org.my-group:my-artifact:1.2.3");
        Assertions.assertTrue(p.matches("org.my-group", "my-artifact", null, "jar", "1.2.3"));
        Assertions.assertFalse(p.matches("org.my-group", "my-artifact", null, "jar", "1.2.4"));
    }

    @Test
    public void gatcv() {
        {
            final ArtifactCoordsPattern p = ArtifactCoordsPattern.of("org.my-group:my-artifact:*:*:1.2.3");
            Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "foo", "jar", "1.2.3"));
            Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "", "jar", "1.2.3"));
            Assertions.assertTrue(p.matches("org.my-group", "my-artifact", null, "jar", "1.2.3"));
            Assertions.assertFalse(p.matches("org.my-group", "my-artifact", null, "jar", "1.2.4"));
        }
        {
            final ArtifactCoordsPattern p = ArtifactCoordsPattern.of("org.my-group:my-artifact:foo:*:1.2.3");
            Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "foo", "jar", "1.2.3"));
            Assertions.assertFalse(p.matches("org.my-group", "my-artifact", "", "jar", "1.2.3"));
            Assertions.assertFalse(p.matches("org.my-group", "my-artifact", null, "jar", "1.2.3"));
            Assertions.assertFalse(p.matches("org.my-group", "my-artifact", null, "jar", "1.2.4"));
        }
    }

    @Test
    public void gatv() {
        try {
            ArtifactCoordsPattern.of("org.my-group:my-artifact:jar:1.2.3");
            Assertions.fail("Expected IllegalStateException for 'org.my-group:my-artifact:jar:1.2.3'");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void ofToString() {
        assertOfToString("org.my-group");
        assertOfToString("org.my-group*");
        assertOfToString("org.my-group:my-artifact");
        assertOfToString("org.my-group:my-artifact:1.2.3");
        Assertions.assertEquals(
                "org.my-group:my-artifact:1.2.3",
                ArtifactCoordsPattern.of("org.my-group:my-artifact:*:*:1.2.3").toString());
        try {
            assertOfToString("org.my-group:my-artifact:foo:1.2.3");
            Assertions.fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
        Assertions.assertEquals(
                "org.my-group:my-artifact:1.2.3",
                ArtifactCoordsPattern.of("org.my-group:my-artifact:*:*:1.2.3").toString());
        assertOfToString("org.my-group:my-artifact:jar:foo:1.2.3");
    }

    static void assertOfToString(String pattern) {
        Assertions.assertEquals(pattern, ArtifactCoordsPattern.of(pattern).toString());
    }
}
