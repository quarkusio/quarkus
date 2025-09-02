package io.quarkus.deployment.pkg.steps;

import static io.quarkus.runtime.graal.GraalVM.Distribution.GRAALVM;
import static io.quarkus.runtime.graal.GraalVM.Distribution.LIBERICA;
import static io.quarkus.runtime.graal.GraalVM.Distribution.MANDREL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.deployment.builditem.nativeimage.NativeMinimalJavaVersionBuildItem;
import io.quarkus.deployment.pkg.steps.GraalVM.Version;
import io.quarkus.runtime.graal.GraalVM.Distribution;

public class GraalVMTest {

    public static final String GRAALVM_VENDOR_VERSION_PROP = "org.graalvm.vendorversion";

    @AfterAll
    public static void tearDownAfterAll() {
        System.clearProperty(GRAALVM_VENDOR_VERSION_PROP);
    }

    @Test
    public void testGraalVMVersionDetected() {
        // Version detection after JDK 25 which no longer uses a GraalVM version mapping,
        // but uses the JDK version instead
        assertVersion(new Version("GraalVM 26.0.0", "26.0.0", GRAALVM), GRAALVM,
                Version.of(Stream.of(("native-image 26 2026-03-17\n"
                        + "GraalVM Runtime Environment GraalVM CE 26-dev+1.1 (build 26+1-jvmci-b01)\n"
                        + "Substrate VM GraalVM CE 26-dev+1.1 (build 26+1, serial gc)").split("\\n"))));
        assertVersion(new Version("GraalVM 25.0.0", "25.0.0", GRAALVM), MANDREL,
                Version.of(Stream.of(("native-image 25-beta 2025-09-16\n" +
                        "OpenJDK Runtime Environment Mandrel-25.0.0.0-dev2df1dd18912 (build 25-beta+29-ea)\n" +
                        "OpenJDK 64-Bit Server VM Mandrel-25.0.0.0-dev2df1dd18912 (build 25-beta+29-ea, mixed mode)")
                        .split("\n"))));
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
    }

    static void assertVersion(Version graalVmVersion, Distribution distro, Version version) {
        assertThat(graalVmVersion.compareTo(version)).isEqualTo(0);
        assertThat(version.toString()).contains(distro.name());
    }

    @Test
    public void testGraalVM21LibericaVersionParser() {
        Version graalVM21Dev = Version.of(Stream.of(("native-image 21.0.1 2023-10-17\n"
                + "GraalVM Runtime Environment Liberica-NIK-23.1.1-1 (build 21.0.1+12-LTS)\n"
                + "Substrate VM Liberica-NIK-23.1.1-1 (build 21.0.1+12-LTS, serial gc)").split("\\n")));
        assertThat(graalVM21Dev.toString()).contains(LIBERICA.name());
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
        assertThat(graalVM21Dev.toString()).contains(GRAALVM.name());
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
        assertThat(graalVM21Dev.toString()).contains(GRAALVM.name());
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
        assertThat(graalVM22Dev.toString()).contains(GRAALVM.name());
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
        assertThat(graalVMEE22Dev.toString()).contains(GRAALVM.name());
        assertThat(graalVMEE22Dev.getVersionAsString()).isEqualTo("24.0-dev");
        assertThat(graalVMEE22Dev.javaVersion.toString()).isEqualTo("22+25-jvmci-b01");
        assertThat(graalVMEE22Dev.javaVersion.feature()).isEqualTo(22);
        assertThat(graalVMEE22Dev.javaVersion.update()).isEqualTo(0);
    }

    @Test
    public void testGraalVMEA24DevVersionParser() {
        final Version graalVMEA24Dev = Version.of(Stream.of(("native-image 24-ea 2025-03-18\n"
                + "OpenJDK Runtime Environment Oracle GraalVM 24-dev.ea+10.1 (build 24-ea+10-1076)\n"
                + "OpenJDK 64-Bit Server VM Oracle GraalVM 24-dev.ea+10.1 (build 24-ea+10-1076, mixed mode, sharing)")
                .split("\\n")));
        assertThat(graalVMEA24Dev.toString()).contains(GRAALVM.name());
        assertThat(graalVMEA24Dev.getVersionAsString()).isEqualTo("24.2-dev");
        assertThat(graalVMEA24Dev.javaVersion.toString()).isEqualTo("24-ea+10-1076");
        assertThat(graalVMEA24Dev.javaVersion.feature()).isEqualTo(24);
        assertThat(graalVMEA24Dev.javaVersion.update()).isEqualTo(0);
    }

    @Test
    public void testGraalVM23_1CommunityVersionParser() {
        final Version version = Version.of(Stream.of(("native-image 21.0.5-beta 2024-10-15\n"
                + "GraalVM Runtime Environment GraalVM CE 21.0.5-dev.beta+3.1 (build 21.0.5-beta+3-ea)\n"
                + "Substrate VM GraalVM CE 21.0.5-dev.beta+3.1 (build 21.0.5-beta+3-ea, serial gc)")
                .split("\\n")));
        assertThat(version.toString().contains(GRAALVM.name()));
        assertThat(version.getVersionAsString()).isEqualTo("23.1-dev");
        assertThat(version.javaVersion.toString()).isEqualTo("21.0.5-beta+3-ea");
        assertThat(version.javaVersion.feature()).isEqualTo(21);
        assertThat(version.javaVersion.interim()).isEqualTo(0);
        assertThat(version.javaVersion.update()).isEqualTo(5);
    }

    @Test
    public void testGraalVM26CommunityVersionParser() {
        final Version version = Version.of(Stream.of(("native-image 26 2026-03-17\n"
                + "GraalVM Runtime Environment GraalVM CE 26-dev+1.1 (build 26+1-jvmci-b01)\n"
                + "Substrate VM GraalVM CE 26-dev+1.1 (build 26+1, serial gc)").split("\\n")));
        assertThat(version.toString().contains(GRAALVM.name()));
        assertThat(version.getVersionAsString()).isEqualTo("26.0.0-dev");
        assertThat(version.javaVersion.toString()).isEqualTo("26+1-jvmci-b01");
        assertThat(version.javaVersion.feature()).isEqualTo(26);
        assertThat(version.javaVersion.interim()).isEqualTo(0);
        assertThat(version.javaVersion.update()).isEqualTo(0);
    }

    @Test
    public void testGraalVMVersionsOlderThan() {
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

    /*
     * Exercise the code path used at native-image build time where the org.graalvm.vendorversion
     * property is being fed to the GraalVM version parsing machinery.
     */
    @ParameterizedTest
    // @formatter:off
    @ValueSource(strings = {
    // GraalVM CE/Community
    "GraalVM CE 26-dev+1.1 |26.0|26|0",
    "GraalVM CE 25-dev+26.1|25.0|25|0",
    "GraalVM CE 24.0.1+9.1 |24.2|24|1",
    // Mandrel
    "Mandrel-25.0.0.0-dev2df1dd18912|25.0|25|0",
    "Mandrel-24.2.1.0-Final|24.2|24|1",
    "Mandrel-23.1.7.0-1b2  |23.1|21|7",
    // Liberica-NIK
    "Liberica-NIK-23.1.7-1 |23.1|21|7",
    "Liberica-NIK-24.2.1-1 |24.2|24|1",
    })
    // @formatter:on
    public void testGraalVMRuntimeVersion(String param) {
        String tokens[] = param.split("\\|", 4);
        Assertions.assertTrue(tokens.length == 4);
        String propertyVal = tokens[0].trim();
        String expectedMajorMinor = tokens[1].trim();
        int expectedJDKFeature = Integer.parseInt(tokens[2].trim());
        int expectedJDKUpdate = Integer.parseInt(tokens[3].trim());

        System.setProperty(GRAALVM_VENDOR_VERSION_PROP, propertyVal);
        io.quarkus.runtime.graal.GraalVM.Version v = io.quarkus.runtime.graal.GraalVM.Version.getCurrent();
        Assertions.assertEquals(expectedMajorMinor, v.getMajorMinorAsString());
        Assertions.assertEquals(expectedJDKFeature, v.javaVersion.feature());
        Assertions.assertEquals(expectedJDKUpdate, v.javaVersion.update());
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
        // Don't ignore micro versions
        assertCompareNotEqualTo("native-image 21.0.1 2025-04-15\n" +
                "OpenJDK Runtime Environment Mandrel-23.1.0.0-Final (build 21.0.1+6-LTS)\n" +
                "OpenJDK 64-Bit Server VM Mandrel-23.1.0.0-Final (build 21.0.1+6-LTS, mixed mode)",
                "native-image 21.0.7 2025-04-15\n" +
                        "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS)\n" +
                        "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS, mixed mode)");
        // Trailing zeros don't affect comparison
        assertCompareEqualTo("native-image 21.0.7 2025-04-15\n" +
                "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS)\n" +
                "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS, mixed mode)",
                "native-image 21.0.7 2025-04-15\n" +
                        "OpenJDK Runtime Environment Mandrel-23.1.7.0.0-Final (build 21.0.7+6-LTS)\n" +
                        "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0.0-Final (build 21.0.7+6-LTS, mixed mode)");
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
        assertThat(Version.of(Stream.of(version.split("\\n"))).compareTo(Version.of(Stream.of(other.split("\\n")))))
                .isNotEqualTo(0);
    }

    @Test
    public void testGraalVMVersionsNewerThan() {
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
    "native-image 17.0.8 2023-07-18\n" +
            "GraalVM Runtime Environment GraalVM CE 17.0.8+7.1 (build 17.0.8+7-jvmci-23.0-b15)\n" +
            "Substrate VM GraalVM CE 17.0.8+7.1 (build 17.0.8+7, serial gc)                                 |17| 8|",
    "native-image 17.0.13 2024-10-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.0.6.0-Final (build 17.0.13+11)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.0.6.0-Final (build 17.0.13+11, mixed mode)                 |17|13|",
    "native-image 25-beta 2025-09-16\n" +
            "OpenJDK Runtime Environment Mandrel-26.0.0-devaefacfb7d2d (build 25-beta+26-ea)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-26.0.0-devaefacfb7d2d (build 25-beta+26-ea, mixed mode)       |25| 0|",
    "native-image 21 2023-09-19\n" +
            "GraalVM Runtime Environment GraalVM CE 21+35.1 (build 21+35-jvmci-23.1-b15)\n" +
            "Substrate VM GraalVM CE 21+35.1 (build 21+35, serial gc)                                       |21| 0|",
    "native-image 21-beta 2023-09-19\n" +
            "OpenJDK Runtime Environment Mandrel-23.0.0.1-devde078ae3bea (build 21-beta+35-ea)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.0.0.1-devde078ae3bea (build 21-beta+35-ea, mixed mode)     |21| 0|",
    "native-image 21.0.7 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS, mixed mode)               |21| 7|",
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
    @ValueSource( strings = {
    "native-image 21.0.7 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS, mixed mode)" +
            "|>|" +
            "native-image 21.0.2 2024-01-16\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-23.1-b30)\n" +
            "Substrate VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13, serial gc)",
    "native-image 21.0.3 2024-01-11\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.3+3.1 (build 21.0.3+3-jvmci-23.1-b30)\n" +
            "Substrate VM GraalVM CE 21.0.3+3.1 (build 21.0.3+3, serial gc)" +
            "|>|" +
            "native-image 21.0.2 2024-01-16\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-23.1-b30)\n" +
            "Substrate VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13, serial gc)",
    "native-image 21.0.2 2024-01-16\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-23.1-b30)\n" +
            "Substrate VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13, serial gc)" +
            "|==|" +
            "native-image 21.0.2 2024-01-16\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-23.1-b32)\n" +
            "Substrate VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13, serial gc)",
    "native-image 21.0.2 2024-01-16\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-23.1-b30)\n" +
            "Substrate VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13, serial gc)" +
            "|==|" +
            "native-image 21.0.2 2024-01-16\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-22.1-b32)\n" +
            "Substrate VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13, serial gc)",
    "native-image 21.0.2 2024-01-16\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-23.1-b30)\n" +
            "Substrate VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13, serial gc)" +
            "|==|" +
            "native-image 21.0.2 2024-01-16\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-23.1-b30)\n" +
            "Substrate VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13, serial gc)",
    "native-image 21.0.7 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS, mixed mode)" +
            "|==|" +
            "native-image 21.0.7 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.2-Final (build 21.0.7+6-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.2-Final (build 21.0.7+6-LTS, mixed mode)",
    "native-image 21.0.7 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS, mixed mode)" +
            "|<|" +
            "native-image 21.0.7 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+9-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+9-LTS, mixed mode)",
    "native-image 21.0.7 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+16-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+16-LTS, mixed mode)" +
            "|<|" +
            "native-image 21.0.8 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.8+9-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.8+9-LTS, mixed mode)",
    "native-image 21.0.7 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+6-LTS, mixed mode)" +
            "|<|" +
            "native-image 21.0.8 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.2-Final (build 21.0.8+6-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.2-Final (build 21.0.8+6-LTS, mixed mode)",
    "native-image 21 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21-internal+0-adhoc.karm.jdk21u)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21-internal+0-adhoc.karm.jdk21u, mixed mode)" +
            "|<|" +
            "native-image 21.0.8 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.2-Final (build 21.0.8+6-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.2-Final (build 21.0.8+6-LTS, mixed mode)",
    "native-image 21.0.2 2024-01-16\n" +
            "GraalVM Runtime Environment GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-23.1-b30)\n" +
            "Substrate VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13, serial gc)" +
            "|<|" +
            "native-image 21.0.7 2025-04-15\n" +
            "OpenJDK Runtime Environment Mandrel-23.1.7.0-Final (build 21.0.7+9-LTS)\n" +
            "OpenJDK 64-Bit Server VM Mandrel-23.1.7.0-Final (build 21.0.7+9-LTS, mixed mode)",
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
