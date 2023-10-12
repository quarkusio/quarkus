package io.quarkus.deployment.pkg.steps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GraalVM {

    // Implements version parsing after https://github.com/oracle/graal/pull/6302
    static final class VersionParseHelper {

        private static final String JVMCI_BUILD_PREFIX = "jvmci-";
        private static final String MANDREL_VERS_PREFIX = "Mandrel-";

        // Java version info (suitable for Runtime.Version.parse()). See java.lang.VersionProps
        private static final String VNUM = "(?<VNUM>[1-9][0-9]*(?:(?:\\.0)*\\.[1-9][0-9]*)*)";
        private static final String PRE = "(?:-(?<PRE>[a-zA-Z0-9]+))?";
        private static final String BUILD = "(?:(?<PLUS>\\+)(?<BUILD>0|[1-9][0-9]*)?)?";
        private static final String OPT = "(?:-(?<OPT>[-a-zA-Z0-9.]+))?";
        private static final String VSTR_FORMAT = VNUM + PRE + BUILD + OPT;

        private static final String VNUM_GROUP = "VNUM";
        private static final String VENDOR_VERSION_GROUP = "VENDOR";
        private static final String BUILD_INFO_GROUP = "BUILDINFO";

        private static final String VENDOR_VERS = "(?<VENDOR>.*)";
        private static final String JDK_DEBUG = "[^\\)]*"; // zero or more of >anything not a ')'<
        private static final String RUNTIME_NAME = "(?<RUNTIME>(?:OpenJDK|GraalVM) Runtime Environment) ";
        private static final String BUILD_INFO = "(?<BUILDINFO>.*)";
        private static final String VM_NAME = "(?<VM>(?:OpenJDK 64-Bit Server|Substrate) VM) ";

        private static final String FIRST_LINE_PATTERN = "native-image " + VSTR_FORMAT + " .*$";
        private static final String SECOND_LINE_PATTERN = RUNTIME_NAME
                + VENDOR_VERS + " \\(" + JDK_DEBUG + "build " + BUILD_INFO + "\\)$";
        private static final String THIRD_LINE_PATTERN = VM_NAME + VENDOR_VERS + " \\(" + JDK_DEBUG + "build .*\\)$";
        private static final Pattern FIRST_PATTERN = Pattern.compile(FIRST_LINE_PATTERN);
        private static final Pattern SECOND_PATTERN = Pattern.compile(SECOND_LINE_PATTERN);
        private static final Pattern THIRD_PATTERN = Pattern.compile(THIRD_LINE_PATTERN);

        private static final String VERS_FORMAT = "(?<VERSION>[1-9][0-9]*(\\.[0-9]+)+(-dev\\p{XDigit}*)?)";
        private static final String VERSION_GROUP = "VERSION";
        private static final Pattern VERSION_PATTERN = Pattern.compile(VERS_FORMAT);

        private static final Version UNKNOWN_VERSION = null;

        static Version parse(List<String> lines) {
            Matcher firstMatcher = FIRST_PATTERN.matcher(lines.get(0));
            Matcher secondMatcher = SECOND_PATTERN.matcher(lines.get(1));
            Matcher thirdMatcher = THIRD_PATTERN.matcher(lines.get(2));
            if (firstMatcher.find() && secondMatcher.find() && thirdMatcher.find()) {
                String javaVersion = firstMatcher.group(VNUM_GROUP);
                java.lang.Runtime.Version v = null;
                try {
                    v = java.lang.Runtime.Version.parse(javaVersion);
                } catch (IllegalArgumentException e) {
                    return UNKNOWN_VERSION;
                }

                String vendorVersion = secondMatcher.group(VENDOR_VERSION_GROUP);

                String buildInfo = secondMatcher.group(BUILD_INFO_GROUP);
                String graalVersion = graalVersion(buildInfo, v.feature());
                if (vendorVersion.contains("-dev")) {
                    graalVersion = graalVersion + "-dev";
                }
                String mandrelVersion = mandrelVersion(vendorVersion);
                Distribution dist = isMandrel(vendorVersion) ? Distribution.MANDREL : Distribution.GRAALVM;
                String versNum = (dist == Distribution.MANDREL ? mandrelVersion : graalVersion);
                if (versNum == null) {
                    return UNKNOWN_VERSION;
                }
                return new Version(lines.stream().collect(Collectors.joining("\n")),
                        versNum, v.feature(), v.update(), dist);
            } else {
                return UNKNOWN_VERSION;
            }
        }

        private static boolean isMandrel(String vendorVersion) {
            if (vendorVersion == null) {
                return false;
            }
            return !vendorVersion.isBlank() && vendorVersion.startsWith(MANDREL_VERS_PREFIX);
        }

        private static String mandrelVersion(String vendorVersion) {
            if (vendorVersion == null) {
                return null;
            }
            int idx = vendorVersion.indexOf(MANDREL_VERS_PREFIX);
            if (idx < 0) {
                return null;
            }
            String version = vendorVersion.substring(idx + MANDREL_VERS_PREFIX.length());
            return matchVersion(version);
        }

        private static String matchVersion(String version) {
            Matcher versMatcher = VERSION_PATTERN.matcher(version);
            if (versMatcher.find()) {
                return versMatcher.group(VERSION_GROUP);
            }
            return null;
        }

        private static String graalVersion(String buildInfo, int jdkFeature) {
            if (buildInfo == null) {
                return null;
            }
            int idx = buildInfo.indexOf(JVMCI_BUILD_PREFIX);
            if (idx < 0) {
                return null;
            }
            String version = buildInfo.substring(idx + JVMCI_BUILD_PREFIX.length());
            Matcher versMatcher = VERSION_PATTERN.matcher(version);
            if (versMatcher.find()) {
                return matchVersion(version);
            } else {
                return GRAAL_MAPPING.get(jdkFeature);
            }
        }
    }

    // Temporarily work around https://github.com/quarkusio/quarkus/issues/36246,
    // till we have a consensus on how to move forward in
    // https://github.com/quarkusio/quarkus/issues/34161
    private static final Map<Integer, String> GRAAL_MAPPING = Map.of(22, "24.0",
            23, "24.1",
            24, "25.0",
            25, "25.1");

    public static final class Version implements Comparable<Version> {

        /**
         * JDK version used with native-image tool:
         * e.g. JDK 17.0.1 is Feature version 17, Update version 1.
         * * Feature: e.g. 11 as in JDK 11, JDK 17, JDK 18 etc.
         * * Interim: 0 so far for the JDK versions we care about, not used here
         * * Update: quarterly updates, e.g. 13 as in JDK 11.0.13.
         * * Patch: emergency release, critical patch, not used here
         */
        private static final Pattern OLD_VERS_PATTERN = Pattern.compile(
                "(GraalVM|native-image)( Version)? " + VersionParseHelper.VERS_FORMAT + "(?<distro>.*?)?" +
                        "(\\(Java Version (?<jfeature>[0-9]+)(\\.(?<jinterim>[0-9]*)\\.(?<jupdate>[0-9]*))?.*)?$");

        static final Version VERSION_21_3 = new Version("GraalVM 21.3", "21.3", Distribution.GRAALVM);
        static final Version VERSION_21_3_0 = new Version("GraalVM 21.3.0", "21.3.0", Distribution.GRAALVM);
        public static final Version VERSION_22_3_0 = new Version("GraalVM 22.3.0", "22.3.0", Distribution.GRAALVM);
        public static final Version VERSION_22_2_0 = new Version("GraalVM 22.2.0", "22.2.0", Distribution.GRAALVM);
        public static final Version VERSION_23_0_0 = new Version("GraalVM 23.0.0", "23.0.0", Distribution.GRAALVM);
        public static final Version VERSION_23_1_0 = new Version("GraalVM 23.1.0", "23.1.0", Distribution.GRAALVM);

        public static final Version MINIMUM = VERSION_22_2_0;
        public static final Version CURRENT = VERSION_23_1_0;
        public static final int UNDEFINED = -1;

        final String fullVersion;
        public final int javaFeatureVersion;
        public final int javaUpdateVersion;
        final Distribution distribution;
        private int[] versions;
        private String suffix;

        Version(String fullVersion, String version, Distribution distro) {
            this(fullVersion, version, 11, UNDEFINED, distro);
        }

        Version(String fullVersion, String version, int javaFeatureVersion, int javaUpdateVersion, Distribution distro) {
            this.fullVersion = fullVersion;
            breakdownVersion(version);
            this.javaFeatureVersion = javaFeatureVersion;
            this.javaUpdateVersion = javaUpdateVersion;
            this.distribution = distro;
        }

        private void breakdownVersion(String version) {
            int dash = version.indexOf('-');
            if (dash != -1) {
                this.suffix = version.substring(dash + 1);
                version = version.substring(0, dash);
            }
            this.versions = Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
        }

        String getFullVersion() {
            return fullVersion;
        }

        boolean isObsolete() {
            return this.compareTo(MINIMUM) < 0;
        }

        boolean isMandrel() {
            return distribution == Distribution.MANDREL;
        }

        boolean isNewerThan(Version version) {
            return this.compareTo(version) > 0;
        }

        boolean isOlderThan(Version version) {
            return this.compareTo(version) < 0;
        }

        /**
         * e.g. JDK 11.0.13 > 11.0.12, 17.0.1 > 11.0.13,
         */
        public boolean jdkVersionGreaterOrEqualTo(int feature, int update) {
            return this.javaFeatureVersion > feature
                    || (this.javaFeatureVersion == feature && this.javaUpdateVersion >= update);
        }

        boolean is(Version version) {
            return this.compareTo(version) == 0;
        }

        @Override
        public int compareTo(Version o) {
            int i = 0;
            for (; i < this.versions.length; i++) {
                if (i >= o.versions.length) {
                    if (this.versions[i] != 0) {
                        return 1;
                    }
                } else if (this.versions[i] != o.versions[i]) {
                    return this.versions[i] - o.versions[i];
                }
            }
            for (; i < o.versions.length; i++) {
                if (o.versions[i] != 0) {
                    return -1;
                }
            }
            return 0;
        }

        public static Version of(Stream<String> output) {
            String stringOutput = output.collect(Collectors.joining("\n"));
            List<String> lines = stringOutput.lines()
                    .dropWhile(l -> !l.startsWith("GraalVM") && !l.startsWith("native-image"))
                    .collect(Collectors.toUnmodifiableList());

            if (lines.size() == 3) {
                // Attempt to parse the new 3-line version scheme first.
                Version parsedVersion = VersionParseHelper.parse(lines);
                if (parsedVersion != VersionParseHelper.UNKNOWN_VERSION) {
                    return parsedVersion;
                }
            } else if (lines.size() == 1) {
                // Old, single line version parsing logic
                final String line = lines.get(0);
                final Matcher oldVersMatcher = OLD_VERS_PATTERN.matcher(line);
                if (oldVersMatcher.find()) {
                    // GraalVM/Mandrel old, single line, version scheme:
                    final String version = oldVersMatcher.group(VersionParseHelper.VERSION_GROUP);
                    final String distro = oldVersMatcher.group("distro");
                    // JDK:
                    // e.g. JDK 17.0.1, feature: 17, interim: 0 (not used here), update: 1
                    final String jFeatureMatch = oldVersMatcher.group("jfeature");
                    final int jFeature = jFeatureMatch == null ? // Old GraalVM versions, like 19, didn't report the Java version.
                            11 : Integer.parseInt(jFeatureMatch);
                    final String jUpdateMatch = oldVersMatcher.group("jupdate");
                    final int jUpdate = jUpdateMatch == null ? // Some JDK dev builds don't report full version string.
                            UNDEFINED : Integer.parseInt(jUpdateMatch);

                    return new Version(
                            line,
                            version,
                            jFeature,
                            jUpdate,
                            isMandrel(distro) ? Distribution.MANDREL : Distribution.GRAALVM);
                }
            }

            throw new IllegalArgumentException(
                    "Cannot parse version from output: \n" + stringOutput);
        }

        private static boolean isMandrel(String s) {
            return s != null && s.contains("Mandrel Distribution");
        }

        /**
         * Returns the Mandrel/GraalVM version as a string. e.g. 21.3.0-rc1
         */
        public String getVersionAsString() {
            String version = Arrays.stream(versions).mapToObj(Integer::toString).collect(Collectors.joining("."));
            if (suffix != null) {
                return version + "-" + suffix;
            }
            return version;
        }

        @Override
        public String toString() {
            return "Version{" +
                    "version="
                    + getVersionAsString() +
                    ", fullVersion=" + fullVersion +
                    ", distribution=" + distribution +
                    ", javaFeatureVersion=" + javaFeatureVersion +
                    ", javaUpdateVersion=" + javaUpdateVersion +
                    '}';
        }

        public boolean isJava17() {
            return javaFeatureVersion == 17;
        }
    }

    enum Distribution {
        GRAALVM,
        MANDREL;
    }
}
