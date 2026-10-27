package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for the resolution of the Java version targeted by multi-release JAR entries.
 */
class JarTreeShakeInputMultiReleaseVersionTest {

    @Test
    void unversionedEntries() {
        assertThat(JarTreeShakeInput.multiReleaseVersion("org/acme/Foo.class")).isZero();
        assertThat(JarTreeShakeInput.multiReleaseVersion("module-info.class")).isZero();
        // only META-INF/versions/ is a version directory
        assertThat(JarTreeShakeInput.multiReleaseVersion("META-INF/versionsfoo/bar/Baz.class")).isZero();
    }

    @Test
    void versionedEntries() {
        assertThat(JarTreeShakeInput.multiReleaseVersion("META-INF/versions/9/org/acme/Foo.class")).isEqualTo(9);
        assertThat(JarTreeShakeInput.multiReleaseVersion("META-INF/versions/21/org/acme/Foo.class")).isEqualTo(21);
        assertThat(JarTreeShakeInput.multiReleaseVersion("META-INF/versions/11/module-info.class")).isEqualTo(11);
    }

    @Test
    void entriesIgnoredByTheJvm() {
        assertThat(JarTreeShakeInput.multiReleaseVersion("META-INF/versions/x/org/acme/Foo.class")).isEqualTo(-1);
        assertThat(JarTreeShakeInput.multiReleaseVersion("META-INF/versions/8/org/acme/Foo.class")).isEqualTo(-1);
        assertThat(JarTreeShakeInput.multiReleaseVersion("META-INF/versions/Foo.class")).isEqualTo(-1);
    }
}
