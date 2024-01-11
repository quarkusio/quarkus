package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.GraalVM.Distribution.GRAALVM;
import static io.quarkus.deployment.pkg.steps.GraalVM.Distribution.MANDREL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.deployment.builditem.nativeimage.NativeMinimalJavaVersionBuildItem;
import io.quarkus.deployment.pkg.steps.GraalVM.Distribution;
import io.quarkus.deployment.pkg.steps.GraalVM.Version;

public class GraalVMTest {

    @Test
    public void testGraalVMVersionDetected() {
        // Version detection after: https://github.com/oracle/graal/pull/6302 (3 lines of version output)
        assertVersion(new Version("GraalVM 23.0.0", "23.0.0", GRAALVM), MANDREL,
                Version.of(Stream.of(("native-image 17.0.6 2023-01-17\n"
                        + "OpenJDK Runtime Environment Mandrel-23.0.0-dev (build 17.0.6+10)\n"
                        + "OpenJDK 64-Bit Server VM Mandrel-23.0.0-dev (build 17.0.6+10, mixed mode)").split("\\n"))));
        assertVersion(new Version("GraalVM 23.0.0", "23.0.0", GRAALVM), MANDREL,
                Version.of(Stream.of(("native-image 17.0.6 2023-01-17\n"
                        + "GraalVM Runtime Environment Mandrel-23.0.0-dev (build 17.0.6+10)\n"
                        + "Substrate VM Mandrel-23.0.0-dev (build 17.0.6+10, serial gc)").split("\\n"))));
        assertVersion(new Version("GraalVM 23.0.0", "23.0.0", GRAALVM), MANDREL,
                Version.of(Stream.of(("native-image 17.0.7 2023-04-18\n"
                        + "OpenJDK Runtime Environment Mandrel-23.0.0.0-Final (build 17.0.7+7)\n"
                        + "OpenJDK 64-Bit Server VM Mandrel-23.0.0.0-Final (build 17.0.7+7, mixed mode)").split("\\n"))));
        // should also work when the image is not around and we have to download it
        assertVersion(new Version("GraalVM 23.0.0", "23.0.0", GRAALVM), MANDREL,
                Version.of(
                        Stream.of(("Unable to find image 'quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-17' locally\n"
                                + "jdk-17: Pulling from quarkus/ubi-quarkus-mandrel-builder-image\n"
                                + "a49367d57626: Already exists\n"
                                + "3a83b3d8356a: Already exists\n"
                                + "d8dd24f2bc88: Already exists\n"
                                + "99bd64fd6c37: Already exists\n"
                                + "7a13281b4a63: Already exists\n"
                                + "ae3db351551e: Already exists\n"
                                + "eee108cfab2c: Already exists\n"
                                + "d38abde1651d: Already exists\n"
                                + "cac4ef5d11c0: Already exists\n"
                                + "89e5b13e9084: Already exists\n"
                                + "68897e59054c: Already exists\n"
                                + "774633828afc: Already exists\n"
                                + "6708d473f3dc: Already exists\n"
                                + "4f4fb700ef54: Already exists\n"
                                + "4f4fb700ef54: Already exists\n"
                                + "Digest: sha256:2833f8ed2cdfcbdf88134a46b5ab7e4dfee8f7cbde60f819f86e59eb73c2491c\n"
                                + "Status: Downloaded newer image for quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-17\n"
                                + "native-image 17.0.7 2023-04-18\n"
                                + "OpenJDK Runtime Environment Mandrel-23.0.0.0-Final (build 17.0.7+7)\n"
                                + "OpenJDK 64-Bit Server VM Mandrel-23.0.0.0-Final (build 17.0.7+7, mixed mode)")
                                .split("\\n"))));
        assertVersion(new Version("GraalVM 23.0", "23.0", GRAALVM), GRAALVM,
                Version.of(Stream.of(("native-image 20 2023-03-21\n"
                        + "GraalVM Runtime Environment GraalVM CE (build 20+34-jvmci-23.0-b10)\n"
                        + "Substrate VM GraalVM CE (build 20+34, serial gc)").split("\\n"))));

        // Should also work for other unknown implementations of GraalVM
        assertVersion(new Version("GraalVM 23.0", "23.0", GRAALVM), GRAALVM,
                Version.of(Stream.of(("native-image 20 2023-07-30\n"
                        + "Foo Runtime Environment whatever (build 20+34-jvmci-23.0-b7)\n"
                        + "Foo VM whatever (build 20+34, serial gc)").split("\\n"))));
        assertVersion(new Version("GraalVM 23.0", "23.0", GRAALVM), GRAALVM,
                Version.of(Stream.of(("native-image 20 2023-07-30\n"
                        + "Another Runtime Environment whatever (build 20+34-jvmci-23.0-b7)\n"
                        + "Another VM whatever (build 20+34, serial gc)").split("\\n"))));

        // Older version parsing
        assertVersion(new Version("GraalVM 20.1", "20.1", GRAALVM), GRAALVM,
                Version.of(Stream.of("GraalVM Version 20.1.0 (Java Version 11.0.7)")));
        assertVersion(new Version("GraalVM 20.1.0.1", "20.1.0.1", GRAALVM), MANDREL, Version
                .of(Stream.of("GraalVM Version 20.1.0.1.Alpha2 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(new Version("GraalVM 20.1.0.1", "20.1.0.1", GRAALVM), MANDREL, Version
                .of(Stream.of("GraalVM Version 20.1.0.1-Final 56d4ee1b28 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(new Version("GraalVM 21.0", "21.0", GRAALVM), MANDREL, Version
                .of(Stream.of("GraalVM Version 21.0.0.0-0b3 (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(new Version("GraalVM 20.3.1.2", "20.3.1.2", GRAALVM), MANDREL, Version
                .of(Stream.of("GraalVM Version 20.3.1.2-dev (Mandrel Distribution) (Java Version 11.0.8)")));
        assertVersion(new Version("GraalVM 21.1", "21.1", GRAALVM), MANDREL, Version
                .of(Stream.of("native-image 21.1.0.0-Final (Mandrel Distribution) (Java Version 11.0.11+9)")));
        assertVersion(new Version("GraalVM 21.1", "21.1", GRAALVM), MANDREL, Version
                .of(Stream.of("GraalVM 21.1.0.0-Final (Mandrel Distribution) (Java Version 11.0.11+9)")));
        assertVersion(new Version("GraalVM 21.1", "21.1", GRAALVM), GRAALVM, Version
                .of(Stream.of("GraalVM 21.1.0 Java 11 CE (Java Version 11.0.11+5-jvmci-21.1-b02)")));
        assertVersion(new Version("GraalVM 21.1", "21.1", GRAALVM), GRAALVM, Version
                .of(Stream.of("native-image 21.1.0.0 Java 11 CE (Java Version 11.0.11+5-jvmci-21.1-b02)")));
        assertVersion(new Version("GraalVM 21.2", "21.2", GRAALVM), MANDREL, Version
                .of(Stream.of("native-image 21.2.0.0-Final Mandrel Distribution (Java Version 11.0.12+7)")));
    }

    static void assertVersion(Version graalVmVersion, Distribution distro, Version version) {
        assertThat(graalVmVersion.compareTo(version)).isEqualTo(0);
        assertThat(version.distribution).isEqualTo(distro);
        if (distro == MANDREL) {
            assertThat(version.isMandrel()).isTrue();
        }
    }

    @Test
    public void testGraalVM21LibericaVersionParser() {
        Version graalVM21Dev = Version.of(Stream.of(("native-image 21.0.1 2023-10-17\n"
                + "GraalVM Runtime Environment Liberica-NIK-23.1.1-1 (build 21.0.1+12-LTS)\n"
                + "Substrate VM Liberica-NIK-23.1.1-1 (build 21.0.1+12-LTS, serial gc)").split("\\n")));
        assertThat(graalVM21Dev.distribution.name()).isEqualTo("LIBERICA");
        assertThat(graalVM21Dev.getVersionAsString()).isEqualTo("23.1.1");
        assertThat(graalVM21Dev.javaVersion.toString()).isEqualTo("21.0.1+12-LTS");
        assertThat(graalVM21Dev.javaVersion.feature()).isEqualTo(21);
        assertThat(graalVM21Dev.javaVersion.update()).isEqualTo(1);
    }

    @Test
    public void testGraalVM21VersionParser() {
        Version graalVM21Dev = Version.of(Stream.of(("native-image 21 2023-09-19\n"
                + "GraalVM Runtime Environment GraalVM CE 21+35.1 (build 21+35-jvmci-23.1-b15)\n"
                + "Substrate VM GraalVM CE 21+35.1 (build 21+35, serial gc)").split("\\n")));
        assertThat(graalVM21Dev.distribution.name()).isEqualTo("GRAALVM");
        assertThat(graalVM21Dev.getVersionAsString()).isEqualTo("23.1");
        assertThat(graalVM21Dev.javaVersion.toString()).isEqualTo("21+35-jvmci-23.1-b15");
        assertThat(graalVM21Dev.javaVersion.feature()).isEqualTo(21);
        assertThat(graalVM21Dev.javaVersion.update()).isEqualTo(0);
    }

    @Test
    public void testGraalVM21DevVersionParser() {
        Version graalVM21Dev = Version.of(Stream.of(("native-image 21 2023-09-19\n" +
                "GraalVM Runtime Environment GraalVM CE 21-dev+35.1 (build 21+35-jvmci-23.1-b14)\n" +
                "Substrate VM GraalVM CE 21-dev+35.1 (build 21+35, serial gc)").split("\\n")));
        assertThat(graalVM21Dev.distribution.name()).isEqualTo("GRAALVM");
        assertThat(graalVM21Dev.getVersionAsString()).isEqualTo("23.1-dev");
        assertThat(graalVM21Dev.javaVersion.toString()).isEqualTo("21+35-jvmci-23.1-b14");
        assertThat(graalVM21Dev.javaVersion.feature()).isEqualTo(21);
        assertThat(graalVM21Dev.javaVersion.update()).isEqualTo(0);
    }

    @Test
    public void testGraalVM22DevVersionParser() {
        Version graalVM22Dev = Version.of(Stream.of(("native-image 22 2024-03-19\n"
                + "GraalVM Runtime Environment GraalVM CE 22-dev+16.1 (build 22+16-jvmci-b01)\n"
                + "Substrate VM GraalVM CE 22-dev+16.1 (build 22+16, serial gc)").split("\\n")));
        assertThat(graalVM22Dev.distribution.name()).isEqualTo("GRAALVM");
        assertThat(graalVM22Dev.getVersionAsString()).isEqualTo("24.0-dev");
        assertThat(graalVM22Dev.javaVersion.toString()).isEqualTo("22+16-jvmci-b01");
        assertThat(graalVM22Dev.javaVersion.feature()).isEqualTo(22);
        assertThat(graalVM22Dev.javaVersion.update()).isEqualTo(0);
    }

    @Test
    public void testGraalVMEE22DevVersionParser() {
        Version graalVMEE22Dev = Version.of(Stream.of(("native-image 22 2024-03-19\n"
                + "Java(TM) SE Runtime Environment Oracle GraalVM 22-dev+25.1 (build 22+25-jvmci-b01)\n"
                + "Java HotSpot(TM) 64-Bit Server VM Oracle GraalVM 22-dev+25.1 (build 22+25-jvmci-b01, mixed mode, sharing)")
                .split("\\n")));
        assertThat(graalVMEE22Dev.distribution.name()).isEqualTo("GRAALVM");
        assertThat(graalVMEE22Dev.getVersionAsString()).isEqualTo("24.0-dev");
        assertThat(graalVMEE22Dev.javaVersion.toString()).isEqualTo("22+25-jvmci-b01");
        assertThat(graalVMEE22Dev.javaVersion.feature()).isEqualTo(22);
        assertThat(graalVMEE22Dev.javaVersion.update()).isEqualTo(0);
    }

    @Test
    public void testGraalVMVersionsOlderThan() {
        assertOlderThan("GraalVM Version 19.3.6 CE", "GraalVM Version 20.2.0 (Java Version 11.0.9)");
        assertOlderThan("GraalVM Version 20.0.0 (Java Version 11.0.7)", "GraalVM Version 20.1.0 (Java Version 11.0.8)");
        assertOlderThan("GraalVM Version 21.2.0 (Java Version 11.0.12)", Version.VERSION_21_3);
        assertOlderThan("GraalVM Version 21.2.0 (Java Version 11.0.12)", Version.VERSION_21_3_0);
        assertOlderThan("native-image 21 2023-09-19\n" +
                "GraalVM Runtime Environment GraalVM CE 21+35.1 (build 21+35-jvmci-23.1-b15)\n" +
                "Substrate VM GraalVM CE 21+35.1 (build 21+35, serial gc)\n",
                "native-image 21-beta 2023-09-19\n" +
                        "OpenJDK Runtime Environment Mandrel-23.1.0.1-devde078ae3bea (build 21-beta+35-ea)\n" +
                        "OpenJDK 64-Bit Server VM Mandrel-23.1.0.1-devde078ae3bea (build 21-beta+35-ea, mixed mode)");
        assertOlderThan("native-image 21-beta 2023-09-19\n" +
                "OpenJDK Runtime Environment Mandrel-23.0.0.1-devde078ae3bea (build 21-beta+35-ea)\n" +
                "OpenJDK 64-Bit Server VM Mandrel-23.0.0.1-devde078ae3bea (build 21-beta+35-ea, mixed mode)",
                "native-image 21 2023-09-19\n" +
                        "GraalVM Runtime Environment GraalVM CE 21+35.1 (build 21+35-jvmci-23.1-b15)\n" +
                        "Substrate VM GraalVM CE 21+35.1 (build 21+35, serial gc)\n");
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
        assertCompareEqualTo("native-image 21-beta 2023-09-19\n" +
                "OpenJDK Runtime Environment Mandrel-23.1.0.0-devde078ae3bea (build 21-beta+35-ea)\n" +
                "OpenJDK 64-Bit Server VM Mandrel-23.1.0.0-devde078ae3bea (build 21-beta+35-ea, mixed mode)",
                "native-image 21 2023-09-19\n" +
                        "GraalVM Runtime Environment GraalVM CE 21+35.1 (build 21+35-jvmci-23.1-b15)\n" +
                        "Substrate VM GraalVM CE 21+35.1 (build 21+35, serial gc)\n");
    }

    /**
     * Asserts that version is equals to other when compared.
     */
    static void assertCompareEqualTo(String version, String other) {
        assertThat(Version.of(Stream.of(version.split("\\n"))).compareTo(Version.of(Stream.of(other.split("\\n")))))
                .isEqualTo(0);
    }

    static void assertCompareNotEqualTo(String version, String other) {
        assertThat(Version.of(Stream.of(version)).compareTo(Version.of(Stream.of(other.split("\\n"))))).isNotEqualTo(0);
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
        assertNewerThan("native-image 22.3.0-dev6d51160e2f3 Mandrel Distribution (Java Version 17.0.4-beta+7-202206162318)",
                "native-image 22.2.0-dev6d51160e2f3 Mandrel Distribution (Java Version 17.0.4-beta+7-202206162318)");
        assertNewerThan("native-image 21-beta 2023-09-19\n" +
                "OpenJDK Runtime Environment Mandrel-23.1.0.1-devde078ae3bea (build 21-beta+35-ea)\n" +
                "OpenJDK 64-Bit Server VM Mandrel-23.1.0.1-devde078ae3bea (build 21-beta+35-ea, mixed mode)",
                "native-image 21 2023-09-19\n" +
                        "GraalVM Runtime Environment GraalVM CE 21+35.1 (build 21+35-jvmci-23.1-b15)\n" +
                        "Substrate VM GraalVM CE 21+35.1 (build 21+35, serial gc)\n");
        assertNewerThan("native-image 21 2023-09-19\n" +
                "GraalVM Runtime Environment GraalVM CE 21+35.1 (build 21+35-jvmci-23.1-b15)\n" +
                "Substrate VM GraalVM CE 21+35.1 (build 21+35, serial gc)\n",
                "native-image 21-beta 2023-09-19\n" +
                        "OpenJDK Runtime Environment Mandrel-23.0.0.1-devde078ae3bea (build 21-beta+35-ea)\n" +
                        "OpenJDK 64-Bit Server VM Mandrel-23.0.0.1-devde078ae3bea (build 21-beta+35-ea, mixed mode)");
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
    "GraalVM Version 19.3.0 CE                                                                      |11|0|",
    "GraalVM Version 20.1.0.4.Final (Mandrel Distribution) (Java Version 11.0.10+9)                 |11|10|",
    "GraalVM Version 20.3.3.0-0b1 (Mandrel Distribution) (Java Version 11.0.12+7-LTS)               |11|12|",
    "native-image 21.1.0.0-Final (Mandrel Distribution) (Java Version 11.0.11+9)                    |11|11|",
    "native-image 21.2.0.2-0b3 Mandrel Distribution (Java Version 11.0.13+8-LTS)                    |11|13|",
    "native-image 21.3.0.0-Final Mandrel Distribution (Java Version 11.0.13+8)                      |11|13|",
    "native-image 21.3.0.0-Final Mandrel Distribution (Java Version 17-internal+0-adhoc.karm.jdk17u)|17|0|",
    "native-image 21.3.0.0-Final Mandrel Distribution (Java Version 17.0.1+12)                      |17| 1|",
    })
    // @formatter:on
    public void testJDKVersion(String s) {
        final String[] versions = s.split("\\|");
        final Version v = Version.of(Stream.of(versions[0].trim()));
        final int expectedJdkFeature = Integer.parseInt(versions[1].trim());
        final int expectedJdkUpdate = Integer.parseInt(versions[2].trim());
        Assertions.assertEquals(expectedJdkFeature, v.javaVersion.feature(), "JDK feature version mismatch.");
        Assertions.assertEquals(expectedJdkUpdate, v.javaVersion.update(), "JDK update version mismatch.");
    }

    @ParameterizedTest
    // @formatter:off
    @ValueSource(strings = {
    "GraalVM 21.3.0 Java 17 CE (Java Version 17.0.1+12-jvmci-21.3-b05)                               |> | GraalVM 21.3.0 Java 11 CE (Java Version 11.0.13+7-jvmci-21.3-b05)",
    "GraalVM 22.0.0-dev Java 11 CE (Java Version 11.0.13+8-jvmci-22.0-b02)                           |< | GraalVM 22.0.0-dev Java 11 CE (Java Version 11.0.14+3-jvmci-22.0-b02)",
    "GraalVM 22.0.0-dev Java 11 CE (Java Version 11.0.13+8-jvmci-22.0-b02)                           |==| GraalVM 22.0.0-dev Java 11 CE (Java Version 11.0.13+8-jvmci-22.0-b04)",
    "GraalVM 22.0.0-dev Java 11 CE (Java Version 11.0.13+8-jvmci-22.0-b02)                           |==| GraalVM 22.0.0-dev Java 11 CE (Java Version 11.0.13+8-jvmci-21.0-b04)",
    "GraalVM Version 19.3.0 CE                                                                       |==| GraalVM Version 19.0.0 CE",
    "native-image 21.3.0.0-Final Mandrel Distribution (Java Version 17-internal+0-adhoc.karm.jdk17u) |< | native-image 21.3.0.0-Final Mandrel Distribution (Java Version 17.0.1+12)",
    "GraalVM 20.3.3 Java 11 (Java Version 11.0.12+6-jvmci-20.3-b20)                                  |< | GraalVM Version 20.3.3.0-0b1 (Mandrel Distribution) (Java Version 11.0.12+7-LTS)",
    })
    // @formatter:on
    public void testJDKVersionCompare(String s) {
        final String[] versions = s.split("\\|");
        final Version a = Version.of(Stream.of(versions[0].trim()));
        final Version b = Version.of(Stream.of(versions[2].trim()));
        final NativeMinimalJavaVersionBuildItem aMinimal = new NativeMinimalJavaVersionBuildItem(a.javaVersion.toString(), "");
        final NativeMinimalJavaVersionBuildItem bMinimal = new NativeMinimalJavaVersionBuildItem(b.javaVersion.toString(), "");
        final String cmp = versions[1].trim();
        switch (cmp) {
            case "<":
                Assertions.assertTrue(b.jdkVersionGreaterOrEqualTo(aMinimal),
                        String.format("JDK %s greater or equal to JDK %s.",
                                b.javaVersion, a.javaVersion));
                Assertions.assertFalse(a.jdkVersionGreaterOrEqualTo(bMinimal),
                        String.format("JDK %s smaller than JDK %s.",
                                a.javaVersion, b.javaVersion));
                break;
            case ">":
                Assertions.assertTrue(a.jdkVersionGreaterOrEqualTo(bMinimal),
                        String.format("JDK %s greater or equal to JDK %s.",
                                a.javaVersion, b.javaVersion));
                Assertions.assertFalse(b.jdkVersionGreaterOrEqualTo(aMinimal),
                        String.format("JDK %s smaller than JDK %s.",
                                b.javaVersion, a.javaVersion));
                break;
            case "==":
                Assertions.assertEquals(0, a.javaVersion.compareToIgnoreOptional(b.javaVersion),
                        String.format("JDK %s equal to JDK %s.",
                                b.javaVersion, a.javaVersion));
                Assertions.assertTrue(a.jdkVersionGreaterOrEqualTo(bMinimal),
                        String.format("JDK %s greater or equal to JDK %s.",
                                a.javaVersion, b.javaVersion));
                break;
            default:
                throw new IllegalArgumentException("Fix the test data. Symbol " + cmp + " is unknown.");
        }
    }

}
