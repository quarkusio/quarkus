package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.GraalVM.Distribution.MANDREL;
import static io.quarkus.deployment.pkg.steps.GraalVM.Distribution.ORACLE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.pkg.steps.GraalVM.Distribution;
import io.quarkus.deployment.pkg.steps.GraalVM.Version;

public class GraalVMTest {

    @Test
    public void testGraalVMVersionDetected() {
        assertVersion(20, 1, ORACLE, Version.of(Stream.of("GraalVM Version 20.1.0 (Java Version 11.0.7)")));
        assertVersion(20, 1, MANDREL, Version
                .of(Stream.of("GraalVM Version 20.1.0.1.Alpha2 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(20, 1, MANDREL, Version
                .of(Stream.of("GraalVM Version 20.1.0.1-Final 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(21, 0, MANDREL, Version
                .of(Stream.of("GraalVM Version 21.0.0.0-0b3 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(20, 3, MANDREL, Version
                .of(Stream.of("GraalVM Version 20.3.1.2-dev (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(21, 1, MANDREL, Version
                .of(Stream.of("native-image 21.1.0.0-Final (Mandrel Distribution) (Java Version 11.0.11+9)")));
        assertVersion(21, 1, MANDREL, Version
                .of(Stream.of("GraalVM 21.1.0.0-Final (Mandrel Distribution) (Java Version 11.0.11+9)")));
        assertVersion(21, 1, ORACLE, Version
                .of(Stream.of("GraalVM 21.1.0 Java 11 CE (Java Version 11.0.11+5-jvmci-21.1-b02)")));
        assertVersion(21, 1, ORACLE, Version
                .of(Stream.of("native-image 21.1.0.0 Java 11 CE (Java Version 11.0.11+5-jvmci-21.1-b02)")));
        assertVersion(21, 2, MANDREL, Version
                .of(Stream.of("native-image 21.2.0.0-Final Mandrel Distribution (Java Version 11.0.12+7)")));
    }

    static void assertVersion(int major, int minor, Distribution distro, Version version) {
        assertThat(version.major).isEqualTo(major);
        assertThat(version.minor).isEqualTo(minor);

        assertThat(version.distribution).isEqualTo(distro);
        if (distro == MANDREL)
            assertThat(version.isMandrel()).isTrue();

        assertThat(version.isDetected()).isEqualTo(true);
    }

    @Test
    public void testGraalVMVersionUndetected() {
        assertThat(Version.of(Stream.of("foo bar")).isDetected()).isFalse();
    }

    @Test
    public void testGraalVMVersionsOlderThan() {
        assertOlderThan("GraalVM Version 19.3.0", "GraalVM Version 20.2.0");
        assertOlderThan("GraalVM Version 20.0.0", "GraalVM Version 20.1.0");
    }

    /**
     * Asserts that version is older than other.
     */
    static void assertOlderThan(String version, String other) {
        assertThat(Version.of(Stream.of(version)).compareTo(Version.of(Stream.of(other)))).isLessThan(0);
    }

    @Test
    public void testGraalVMVersionsCompareEqualTo() {
        assertCompareEqualTo("GraalVM Version 20.1.0", "GraalVM Version 20.1.0");
        // Distributions don't impact newer/older/same comparisons
        assertCompareEqualTo("GraalVM Version 20.1.0",
                "GraalVM Version 20.1.0.1.Alpha2 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)");
        // Micro versions ignored, hence two micros are considered the same right now
        assertCompareEqualTo("GraalVM Version 19.3.0", "GraalVM Version 19.3.3");
    }

    /**
     * Asserts that version is equals to other when compared.
     */
    static void assertCompareEqualTo(String version, String other) {
        assertThat(Version.of(Stream.of(version)).compareTo(Version.of(Stream.of(other)))).isEqualTo(0);
    }

    @Test
    public void testGraalVMVersionsNewerThan() {
        assertNewerThan("GraalVM Version 20.0.0", "GraalVM Version 19.3.0");
        assertNewerThan("GraalVM Version 20.1.0.1.Alpha2 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)",
                "GraalVM Version 20.0.0");
    }

    /**
     * Asserts that version is newer than other.
     */
    static void assertNewerThan(String version, String other) {
        assertThat(Version.of(Stream.of(version)).isNewerThan(Version.of(Stream.of(other)))).isTrue();
    }

}
