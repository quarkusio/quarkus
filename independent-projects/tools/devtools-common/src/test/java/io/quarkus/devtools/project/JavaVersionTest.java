package io.quarkus.devtools.project;

import static io.quarkus.devtools.project.JavaVersion.DETECT_JAVA_RUNTIME_VERSION;
import static io.quarkus.devtools.project.JavaVersion.JAVA_VERSIONS_LTS;
import static io.quarkus.devtools.project.JavaVersion.computeJavaVersion;
import static io.quarkus.devtools.project.JavaVersion.determineBestJavaLtsVersion;
import static io.quarkus.devtools.project.JavaVersion.getCompatibleLTSVersions;
import static io.quarkus.devtools.project.SourceType.JAVA;
import static io.quarkus.devtools.project.SourceType.KOTLIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JavaVersionTest {

    @Test
    public void givenJavaVersion17ShouldReturn17() {
        assertEquals("17", computeJavaVersion(JAVA, "17"));
    }

    @Test
    public void givenJavaVersion16ShouldReturn11() {
        assertEquals("11", computeJavaVersion(JAVA, "16.0.1"));
    }

    @Test
    public void givenJavaVersion11ShouldReturn11() {
        assertEquals("11", computeJavaVersion(JAVA, "11"));
    }

    @Test
    public void givenJavaVersion18ShouldReturn17() {
        assertEquals("17", computeJavaVersion(JAVA, "18"));
    }

    @Test
    void shouldProperlyUseMinJavaVersion() {
        assertThat(getCompatibleLTSVersions(new JavaVersion("11"))).isEqualTo(JAVA_VERSIONS_LTS);
        assertThat(getCompatibleLTSVersions(new JavaVersion("17"))).containsExactly(17, 21);
        assertThat(getCompatibleLTSVersions(new JavaVersion("100"))).isEmpty();
        assertThat(getCompatibleLTSVersions(JavaVersion.NA)).isEqualTo(JAVA_VERSIONS_LTS);
    }

    @Test
    public void givenAutoDetectShouldReturnAppropriateVersion() {
        final String bestJavaLtsVersion = String.valueOf(determineBestJavaLtsVersion(Runtime.version().feature()));
        assertEquals(bestJavaLtsVersion, computeJavaVersion(JAVA, DETECT_JAVA_RUNTIME_VERSION));
    }

    @Test
    public void testDetermineBestLtsVersion() {
        assertEquals(11, determineBestJavaLtsVersion(8));
        assertEquals(11, determineBestJavaLtsVersion(11));
        assertEquals(11, determineBestJavaLtsVersion(12));
        assertEquals(17, determineBestJavaLtsVersion(17));
        assertEquals(17, determineBestJavaLtsVersion(18));
    }

    @Test
    public void givenKotlinProjectWithVersion18ShouldReturn17() {
        assertEquals("17", computeJavaVersion(KOTLIN, "18"));
    }
}
