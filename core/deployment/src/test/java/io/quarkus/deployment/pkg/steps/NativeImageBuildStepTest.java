package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.NativeImageBuildStep.GraalVM.Distribution.MANDREL;
import static io.quarkus.deployment.pkg.steps.NativeImageBuildStep.GraalVM.Distribution.ORACLE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.pkg.steps.NativeImageBuildStep.GraalVM.Distribution;
import io.quarkus.deployment.pkg.steps.NativeImageBuildStep.GraalVM.Version;

public class NativeImageBuildStepTest {

    @Test
    public void testGraalVMVersionDetected() {
        assertVersion(1, 0, ORACLE, Version.of(Stream.of("GraalVM Version 1.0.0")));
        assertVersion(19, 3, ORACLE, Version.of(Stream.of("GraalVM Version 19.3.0")));
        assertVersion(19, 3, ORACLE, Version.of(Stream.of("GraalVM Version 19.3.3")));
        assertVersion(20, 0, ORACLE, Version.of(Stream.of("GraalVM Version 20.0.0")));
        assertVersion(20, 1, ORACLE, Version.of(Stream.of("GraalVM Version 20.1.0 (Java Version 11.0.7)")));
        assertVersion(20, 1, MANDREL, Version
                .of(Stream.of("GraalVM Version 20.1.0.1.Alpha2 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(20, 1, MANDREL, Version
                .of(Stream.of("GraalVM Version 20.1.0.1-Final 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(21, 0, MANDREL, Version
                .of(Stream.of("GraalVM Version 21.0.0.0-0b3 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(20, 3, MANDREL, Version
                .of(Stream.of("GraalVM Version 20.3.1.2-dev (Mandrel Distribution) (Java Version 11.0.8)")));
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
    public void testGraalVMVersionSnapshot() {
        assertSnapshot(MANDREL,
                Version.of(Stream.of("GraalVM Version beb2fd6 (Mandrel Distribution) (Java Version 11.0.9-internal)")));
    }

    static void assertSnapshot(Distribution distro, Version version) {
        assertThat(version.distribution).isEqualTo(distro);
        assertThat(version.isDetected()).isEqualTo(true);
        assertThat(version.isSnapshot()).isEqualTo(true);
    }

    @Test
    public void testGraalVMVersionsOlderThan() {
        assertOlderThan("GraalVM Version 1.0.0", "GraalVM Version 19.3.0");
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
