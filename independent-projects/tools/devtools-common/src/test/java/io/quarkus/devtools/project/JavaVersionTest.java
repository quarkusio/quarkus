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
    public void givenJavaVersion8ShouldReturn8() {
        assertEquals(8, new JavaVersion("8").getAsInt());
        assertEquals(8, new JavaVersion("1.8").getAsInt());
    }

    @Test
    public void givenJavaVersion17ShouldReturn17() {
        assertEquals("17", computeJavaVersion(JAVA, "17"));
    }

    @Test
    public void givenJavaVersion22ShouldReturn21() {
        assertEquals("21", computeJavaVersion(JAVA, "22.0.1"));
    }

    @Test
    public void givenJavaVersion21ShouldReturn21() {
        assertEquals("21", computeJavaVersion(JAVA, "21"));
    }

    @Test
    void shouldProperlyUseMinJavaVersion() {
        assertThat(getCompatibleLTSVersions(new JavaVersion("17"))).containsExactly(17, 21);
        assertThat(getCompatibleLTSVersions(new JavaVersion("21"))).containsExactly(21);
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
        assertEquals(17, determineBestJavaLtsVersion(17));
        assertEquals(17, determineBestJavaLtsVersion(18));
        assertEquals(21, determineBestJavaLtsVersion(21));
        assertEquals(21, determineBestJavaLtsVersion(22));
    }

    @Test
    public void givenKotlinProjectWithVersion18ShouldReturn17() {
        assertEquals("17", computeJavaVersion(KOTLIN, "18"));
    }
}
