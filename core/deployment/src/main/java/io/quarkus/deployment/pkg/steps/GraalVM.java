package io.quarkus.deployment.pkg.steps;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.builditem.nativeimage.NativeMinimalJavaVersionBuildItem;
import io.quarkus.runtime.graal.GraalVM.Distribution;

public final class GraalVM {

    // Implements version parsing after https://github.com/oracle/graal/pull/6302
    static final class VersionParseHelper {

        private static final String EA_BUILD_PREFIX = "-ea";
        private static final String JVMCI_BUILD_PREFIX = "jvmci-";
        private static final String MANDREL_VERS_PREFIX = "Mandrel-";

        private static final String LIBERICA_NIK_VERS_PREFIX = "Liberica-NIK-";

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
        private static final String RUNTIME_NAME = "(?<RUNTIME>(?:.*) Runtime Environment) ";
        private static final String BUILD_INFO = "(?<BUILDINFO>.*)";
        private static final String VM_NAME = "(?<VM>(?:.*) VM) ";

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
                String javaVersion = secondMatcher.group(BUILD_INFO_GROUP);
                java.lang.Runtime.Version v;
                try {
                    v = java.lang.Runtime.Version.parse(javaVersion);
                } catch (IllegalArgumentException e) {
                    return UNKNOWN_VERSION;
                }

                String vendorVersion = secondMatcher.group(VENDOR_VERSION_GROUP);

                String graalVersion = graalVersion(javaVersion, v.feature());
                if (vendorVersion.contains("-dev")) {
                    graalVersion = graalVersion + "-dev";
                }
                String versNum;
                Distribution dist;
                if (isMandrel(vendorVersion)) {
                    dist = Distribution.MANDREL;
                    versNum = mandrelVersion(vendorVersion);
                } else if (isLiberica(vendorVersion)) {
                    dist = Distribution.LIBERICA;
                    versNum = libericaVersion(vendorVersion);
                } else {
                    dist = Distribution.GRAALVM;
                    versNum = graalVersion;
                }
                if (versNum == null) {
                    return UNKNOWN_VERSION;
                }
                return new Version(String.join("\n", lines),
                        versNum, v, dist);
            } else {
                return UNKNOWN_VERSION;
            }
        }

        private static boolean isLiberica(String vendorVersion) {
            if (vendorVersion == null) {
                return false;
            }
            return !vendorVersion.isBlank() && vendorVersion.startsWith(LIBERICA_NIK_VERS_PREFIX);
        }

        private static String libericaVersion(String vendorVersion) {
            if (vendorVersion == null) {
                return null;
            }
            final String version = buildVersion(vendorVersion, LIBERICA_NIK_VERS_PREFIX);
            if (version == null) {
                return null;
            }
            return matchVersion(version);
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
            final String version = buildVersion(vendorVersion, MANDREL_VERS_PREFIX);
            if (version == null) {
                return null;
            }
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
            String version = buildVersion(buildInfo, JVMCI_BUILD_PREFIX);
            if (version == null) {
                version = buildVersion(buildInfo, EA_BUILD_PREFIX);
                if (version == null) {
                    return null;
                }
            }
            Matcher versMatcher = VERSION_PATTERN.matcher(version);
            if (versMatcher.find()) {
                return matchVersion(version);
            } else {
                return Version.GRAAL_MAPPING.get(Integer.toString(jdkFeature));
            }
        }

        private static String buildVersion(String buildInfo, String buildPrefix) {
            int idx = buildInfo.indexOf(buildPrefix);
            if (idx < 0) {
                return null;
            }
            return buildInfo.substring(idx + buildPrefix.length());
        }
    }

    public static final class Version extends io.quarkus.runtime.graal.GraalVM.Version {

        // Get access to GRAAL_MAPPING without making it public
        private static final Map<String, String> GRAAL_MAPPING = io.quarkus.runtime.graal.GraalVM.Version.GRAAL_MAPPING;

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
                        "(\\(Java Version (?<javaversion>(?<jfeature>[0-9]+)(\\.(?<jinterim>[0-9]*)\\.(?<jupdate>[0-9]*))?.*)\\))?$");

        static final Version VERSION_21_3 = new Version("GraalVM 21.3", "21.3", Distribution.GRAALVM);
        static final Version VERSION_21_3_0 = new Version("GraalVM 21.3.0", "21.3.0", Distribution.GRAALVM);
        public static final Version VERSION_23_0_0 = new Version("GraalVM 23.0.0", "23.0.0", "17", Distribution.GRAALVM);
        public static final Version VERSION_23_1_0 = new Version("GraalVM 23.1.0", "23.1.0", "21", Distribution.GRAALVM);
        public static final Version VERSION_23_1_2 = new Version("GraalVM 23.1.2", "23.1.2", "21", Distribution.GRAALVM);
        public static final Version VERSION_23_1_3 = new Version("GraalVM 23.1.3", "23.1.3", "21", Distribution.GRAALVM);
        public static final Version VERSION_24_0_0 = new Version("GraalVM 24.0.0", "24.0.0", "22", Distribution.GRAALVM);
        public static final Version VERSION_24_0_999 = new Version("GraalVM 24.0.999", "24.0.999", "22", Distribution.GRAALVM);
        public static final Version VERSION_24_1_0 = new Version("GraalVM 24.1.0", "24.1.0", "23", Distribution.GRAALVM);

        /**
         * The minimum version of GraalVM supported by Quarkus.
         * Versions prior to this are expected to cause major issues.
         *
         * @deprecated Use {@link io.quarkus.runtime.graal.GraalVM.Version.MINIMUM} instead.
         */
        @Deprecated
        public static final Version MINIMUM = VERSION_23_0_0;
        /**
         * The current version of GraalVM supported by Quarkus.
         * This version is the one actively being tested and is expected to give the best experience.
         *
         * @deprecated Use {@link io.quarkus.runtime.graal.GraalVM.Version.CURRENT} instead.
         */
        @Deprecated
        public static final Version CURRENT = VERSION_23_1_0;
        /**
         * The minimum version of GraalVM officially supported by Quarkus.
         * Versions prior to this are expected to work but are not given the same level of testing or priority.
         *
         * @deprecated Use {@link io.quarkus.runtime.graal.GraalVM.Version.MINIMUM_SUPPORTED} instead.
         */
        @Deprecated
        public static final Version MINIMUM_SUPPORTED = CURRENT;

        Version(String fullVersion, String version, Distribution distro) {
            this(fullVersion, version, "11", distro);
        }

        Version(String fullVersion, String version, String javaVersion, Distribution distro) {
            this(fullVersion, version, Runtime.Version.parse(javaVersion), distro);
        }

        Version(String fullVersion, String version, Runtime.Version javaVersion, Distribution distro) {
            super(fullVersion, version, javaVersion, distro);
        }

        public int compareTo(GraalVM.Version o) {
            return compareTo((io.quarkus.runtime.graal.GraalVM.Version) o);
        }

        Distribution getDistribution() {
            return distribution;
        }

        String getFullVersion() {
            return fullVersion;
        }

        boolean isObsolete() {
            return this.compareTo(io.quarkus.runtime.graal.GraalVM.Version.MINIMUM) < 0;
        }

        boolean isSupported() {
            return this.compareTo(io.quarkus.runtime.graal.GraalVM.Version.MINIMUM_SUPPORTED) >= 0;
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
        public boolean jdkVersionGreaterOrEqualTo(NativeMinimalJavaVersionBuildItem javaVersionBuildItem) {
            return javaVersion.compareToIgnoreOptional(javaVersionBuildItem.minVersion) >= 0;
        }

        public boolean jdkVersionGreaterOrEqualTo(String version) {
            return javaVersion.compareToIgnoreOptional(Runtime.Version.parse(version)) >= 0;
        }

        public static Version of(Stream<String> output) {
            String stringOutput = output.collect(Collectors.joining("\n"));
            List<String> lines = stringOutput.lines()
                    .dropWhile(l -> !l.startsWith("GraalVM") && !l.startsWith("native-image"))
                    .toList();

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
                    String javaVersion = oldVersMatcher.group("javaversion");
                    if (javaVersion == null) {
                        if (version.startsWith("19")) {
                            javaVersion = "11"; // Fallback to JDK 11 for GraalVM 19.x
                        } else {
                            throw new IllegalArgumentException(
                                    "Cannot parse version from output: \n" + stringOutput);
                        }
                    }

                    return new Version(
                            line,
                            version,
                            Runtime.Version.parse(javaVersion),
                            isMandrel(distro) ? Distribution.MANDREL : Distribution.GRAALVM);
                }
            }

            throw new IllegalArgumentException(
                    "Cannot parse version from output: \n" + stringOutput);
        }

        private static boolean isMandrel(String s) {
            return s != null && s.contains("Mandrel Distribution");
        }

        public boolean isJava17() {
            return javaVersion.feature() == 17;
        }
    }
}
