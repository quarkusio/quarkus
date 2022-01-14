package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.GraalVM.Distribution.MANDREL;
import static io.quarkus.deployment.pkg.steps.GraalVM.Distribution.ORACLE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.deployment.pkg.steps.GraalVM.Distribution;
import io.quarkus.deployment.pkg.steps.GraalVM.Version;

public class GraalVMTest {

    @Test
    public void testGraalVMVersionDetected() {
        assertVersion(org.graalvm.home.Version.create(20, 1), ORACLE,
                Version.of(Stream.of("GraalVM Version 20.1.0 (Java Version 11.0.7)")));
        assertVersion(org.graalvm.home.Version.create(20, 1, 0, 1), MANDREL, Version
                .of(Stream.of("GraalVM Version 20.1.0.1.Alpha2 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(org.graalvm.home.Version.create(20, 1, 0, 1), MANDREL, Version
                .of(Stream.of("GraalVM Version 20.1.0.1-Final 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(org.graalvm.home.Version.create(21, 0), MANDREL, Version
                .of(Stream.of("GraalVM Version 21.0.0.0-0b3 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(org.graalvm.home.Version.create(20, 3, 1, 2), MANDREL, Version
                .of(Stream.of("GraalVM Version 20.3.1.2-dev (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(org.graalvm.home.Version.create(21, 1), MANDREL, Version
                .of(Stream.of("native-image 21.1.0.0-Final (Mandrel Distribution) (Java Version 11.0.11+9)")));
        assertVersion(org.graalvm.home.Version.create(21, 1), MANDREL, Version
                .of(Stream.of("GraalVM 21.1.0.0-Final (Mandrel Distribution) (Java Version 11.0.11+9)")));
        assertVersion(org.graalvm.home.Version.create(21, 1), ORACLE, Version
                .of(Stream.of("GraalVM 21.1.0 Java 11 CE (Java Version 11.0.11+5-jvmci-21.1-b02)")));
        assertVersion(org.graalvm.home.Version.create(21, 1), ORACLE, Version
                .of(Stream.of("native-image 21.1.0.0 Java 11 CE (Java Version 11.0.11+5-jvmci-21.1-b02)")));
        assertVersion(org.graalvm.home.Version.create(21, 2), MANDREL, Version
                .of(Stream.of("native-image 21.2.0.0-Final Mandrel Distribution (Java Version 11.0.12+7)")));
    }

    static void assertVersion(org.graalvm.home.Version graalVmVersion, Distribution distro, Version version) {
        assertThat(graalVmVersion.compareTo(version.version)).isEqualTo(0);
        assertThat(version.distribution).isEqualTo(distro);
        if (distro == MANDREL) {
            assertThat(version.isMandrel()).isTrue();
        }
        assertThat(version.isDetected()).isEqualTo(true);
    }

    @Test
    public void testGraalVMVersionUndetected() {
        assertThat(Version.of(Stream.of("foo bar")).isDetected()).isFalse();
    }

    @Test
    public void testGraalVMVersionsOlderThan() {
        assertOlderThan("GraalVM Version 19.3.6 CE", "GraalVM Version 20.2.0 (Java Version 11.0.9)");
        assertOlderThan("GraalVM Version 20.0.0 (Java Version 11.0.7)", "GraalVM Version 20.1.0 (Java Version 11.0.8)");
        assertOlderThan("GraalVM Version 21.2.0 (Java Version 11.0.12)", Version.VERSION_21_3);
        assertOlderThan("GraalVM Version 21.2.0 (Java Version 11.0.12)", Version.VERSION_21_3_0);
    }

    /**
     * Asserts that one version is older than the other.
     */
    static void assertOlderThan(String version, String other) {
        assertThat(Version.of(Stream.of(version)).compareTo(Version.of(Stream.of(other)))).isLessThan(0);
    }

    static void assertOlderThan(String version, GraalVM.Version other) {
        assertThat(Version.of(Stream.of(version)).compareTo(other)).isLessThan(0);
    }

    @Test
    public void testGraalVMVersionsCompareEqualTo() {
        assertCompareEqualTo("GraalVM Version 20.1.0 (Java Version 11.0.8)", "GraalVM Version 20.1.0 (Java Version 11.0.8)");
        // Distributions don't impact newer/older/same comparisons
        assertCompareEqualTo("GraalVM Version 20.1.0.1 (Java Version 11.0.8)",
                "GraalVM Version 20.1.0.1.Alpha2 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)");
        // Don't ignore micro versions
        assertCompareNotEqualTo("GraalVM Version 19.3.0", "GraalVM Version 19.3.6 CE");
        // Trailing zeros don't affect comparison
        assertCompareEqualTo("GraalVM 21.3 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)",
                "GraalVM 21.3.0 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)");
        assertCompareEqualTo("GraalVM 21.3.0 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)",
                "GraalVM 21.3.0.0.0.0 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)");
        assertThat(Version.VERSION_21_3.compareTo(Version.VERSION_21_3_0)).isEqualTo(0);
    }

    /**
     * Asserts that version is equals to other when compared.
     */
    static void assertCompareEqualTo(String version, String other) {
        assertThat(Version.of(Stream.of(version)).compareTo(Version.of(Stream.of(other)))).isEqualTo(0);
    }

    static void assertCompareNotEqualTo(String version, String other) {
        assertThat(Version.of(Stream.of(version)).compareTo(Version.of(Stream.of(other)))).isNotEqualTo(0);
    }

    @Test
    public void testGraalVMVersionsNewerThan() {
        assertNewerThan("GraalVM Version 20.0.0 (Java Version 11.0.7)", "GraalVM Version 19.3.0");
        assertNewerThan("GraalVM Version 20.1.0.1.Alpha2 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)",
                "GraalVM Version 20.0.0 (Java Version 11.0.7)");
        assertNewerThan("GraalVM 21.3.1 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)",
                "GraalVM 21.3.0 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)");
        assertNewerThan("GraalVM 21.3.1 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)",
                "GraalVM 21.3 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)");
    }

    /**
     * Asserts that one version is newer than the other.
     */
    static void assertNewerThan(String version, String other) {
        assertThat(Version.of(Stream.of(version)).isNewerThan(Version.of(Stream.of(other)))).isTrue();
    }

    @ParameterizedTest
    // @formatter:off
    @ValueSource(strings = {
    "GraalVM 20.3.3 Java 11 (Java Version 11.0.12+6-jvmci-20.3-b20)                                 |11|12|",
    "GraalVM 21.1.0 Java 11 CE (Java Version 11.0.11+8-jvmci-21.1-b05)                              |11|11|",
    "GraalVM 21.3.0 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)                              |11|13|",
    "GraalVM 21.3.0 Java 17 CE (Java Version 17.0.1+12-jvmci-21.3-b05)                              |17| 1|",
    "GraalVM 22.0.0-dev Java 11 CE (Java Version 11.0.13+8-jvmci-22.0-b02)                          |11|13|",
    "GraalVM Version 19.3.0 CE                                                                      |11|-1|",
    "GraalVM Version 20.1.0.4.Final (Mandrel Distribution) (Java Version 11.0.10+9)                 |11|10|",
    "GraalVM Version 20.3.3.0-0b1 (Mandrel Distribution) (Java Version 11.0.12+7-LTS)               |11|12|",
    "native-image 21.1.0.0-Final (Mandrel Distribution) (Java Version 11.0.11+9)                    |11|11|",
    "native-image 21.2.0.2-0b3 Mandrel Distribution (Java Version 11.0.13+8-LTS)                    |11|13|",
    "native-image 21.3.0.0-Final Mandrel Distribution (Java Version 11.0.13+8)                      |11|13|",
    "native-image 21.3.0.0-Final Mandrel Distribution (Java Version 17-internal+0-adhoc.karm.jdk17u)|17|-1|",
    "native-image 21.3.0.0-Final Mandrel Distribution (Java Version 17.0.1+12)                      |17| 1|",
    })
    // @formatter:on
    public void testJDKVersion(String s) {
        final String[] versions = s.split("\\|");
        final Version v = Version.of(Stream.of(versions[0].trim()));
        final int expectedJdkFeature = Integer.parseInt(versions[1].trim());
        final int expectedJdkUpdate = Integer.parseInt(versions[2].trim());
        Assertions.assertEquals(expectedJdkFeature, v.javaFeatureVersion, "JDK feature version mismatch.");
        Assertions.assertEquals(expectedJdkUpdate, v.javaUpdateVersion, "JDK feature version mismatch.");
    }

    @ParameterizedTest
    // @formatter:off
    @ValueSource(strings = {
    "GraalVM 21.3.0 Java 17 CE (Java Version 17.0.1+12-jvmci-21.3-b05)                               |> | GraalVM 21.3.0 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)",
    "GraalVM 22.0.0-dev Java 11 CE (Java Version 11.0.13+8-jvmci-22.0-b02)                           |< | GraalVM 22.0.0-dev Java 11 CE (Java Version 11.0.14+3-jvmci-22.0-b02)",
    "GraalVM Version 19.3.0 CE                                                                       |==| GraalVM Version 19.0.0 CE",
    "native-image 21.3.0.0-Final Mandrel Distribution (Java Version 17-internal+0-adhoc.karm.jdk17u) |< | native-image 21.3.0.0-Final Mandrel Distribution (Java Version 17.0.1+12)",
    "GraalVM 20.3.3 Java 11 (Java Version 11.0.12+6-jvmci-20.3-b20)                                  |==| GraalVM Version 20.3.3.0-0b1 (Mandrel Distribution) (Java Version 11.0.12+7-LTS)",
    })
    // @formatter:on
    public void testJDKVersionCompare(String s) {
        final String[] versions = s.split("\\|");
        final Version a = Version.of(Stream.of(versions[0].trim()));
        final Version b = Version.of(Stream.of(versions[2].trim()));
        final String cmp = versions[1].trim();
        switch (cmp) {
            case "<":
                Assertions.assertTrue(b.jdkVersionGreaterOrEqualTo(a.javaFeatureVersion, a.javaUpdateVersion),
                        String.format("JDK %d.0.%d greater or equal to JDK %d.0.%d.",
                                b.javaFeatureVersion, b.javaUpdateVersion, a.javaFeatureVersion, a.javaUpdateVersion));
                break;
            case ">":
                Assertions.assertTrue(a.jdkVersionGreaterOrEqualTo(b.javaFeatureVersion, b.javaUpdateVersion),
                        String.format("JDK %d.0.%d greater or equal to JDK %d.0.%d.",
                                a.javaFeatureVersion, a.javaUpdateVersion, b.javaFeatureVersion, b.javaUpdateVersion));
                break;
            case "==":
                Assertions.assertTrue(
                        a.javaFeatureVersion == b.javaFeatureVersion && a.javaUpdateVersion == b.javaUpdateVersion,
                        String.format("JDK %d.0.%d equal to JDK %d.0.%d.",
                                a.javaFeatureVersion, a.javaUpdateVersion, b.javaFeatureVersion, b.javaUpdateVersion));
                break;
            default:
                throw new IllegalArgumentException("Fix the test data. Symbol " + cmp + " is unknown.");
        }
    }

}
