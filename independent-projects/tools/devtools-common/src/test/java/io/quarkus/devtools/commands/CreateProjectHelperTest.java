package io.quarkus.devtools.commands;

import static io.quarkus.devtools.commands.CreateProjectHelper.DETECT_JAVA_RUNTIME_VERSION;
import static io.quarkus.devtools.commands.CreateProjectHelper.computeJavaVersion;
import static io.quarkus.devtools.commands.CreateProjectHelper.determineBestJavaLtsVersion;
import static io.quarkus.devtools.commands.SourceType.JAVA;
import static io.quarkus.devtools.commands.SourceType.KOTLIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CreateProjectHelperTest {

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
