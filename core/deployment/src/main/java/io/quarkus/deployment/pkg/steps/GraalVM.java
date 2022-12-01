package io.quarkus.deployment.pkg.steps;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class GraalVM {
    public static final class Version implements Comparable<Version> {

        /**
         * JDK version used with native-image tool:
         * e.g. JDK 17.0.1 is Feature version 17, Update version 1.
         * * Feature: e.g. 11 as in JDK 11, JDK 17, JDK 18 etc.
         * * Interim: 0 so far for the JDK versions we care about, not used here
         * * Update: quarterly updates, e.g. 13 as in JDK 11.0.13.
         * * Patch: emergency release, critical patch, not used here
         */
        private static final Pattern PATTERN = Pattern.compile(
                "(GraalVM|native-image)( Version)? (?<version>[1-9][0-9]*(\\.[0-9]+)+(-dev\\p{XDigit}*)?)(?<distro>.*?)?" +
                        "(\\(Java Version (?<jfeature>[0-9]+)(\\.(?<jinterim>[0-9]*)\\.(?<jupdate>[0-9]*))?.*)?$");

        static final Version UNVERSIONED = new Version("Undefined", "snapshot", Distribution.ORACLE);
        static final Version VERSION_21_3 = new Version("GraalVM 21.3", "21.3", Distribution.ORACLE);
        static final Version VERSION_21_3_0 = new Version("GraalVM 21.3.0", "21.3.0", Distribution.ORACLE);
        public static final Version VERSION_22_3_0 = new Version("GraalVM 22.3.0", "22.3.0", Distribution.ORACLE);
        public static final Version VERSION_22_2_0 = new Version("GraalVM 22.2.0", "22.2.0", Distribution.ORACLE);

        public static final Version MINIMUM = VERSION_22_2_0;
        public static final Version CURRENT = VERSION_22_3_0;
        public static final int UNDEFINED = -1;

        final String fullVersion;
        final org.graalvm.home.Version version;
        public final int javaFeatureVersion;
        public final int javaUpdateVersion;
        final Distribution distribution;

        Version(String fullVersion, String version, Distribution distro) {
            this(fullVersion, version, 11, UNDEFINED, distro);
        }

        Version(String fullVersion, String version, int javaFeatureVersion, int javaUpdateVersion, Distribution distro) {
            this.fullVersion = fullVersion;
            this.version = org.graalvm.home.Version.parse(version);
            this.javaFeatureVersion = javaFeatureVersion;
            this.javaUpdateVersion = javaUpdateVersion;
            this.distribution = distro;
        }

        String getFullVersion() {
            return fullVersion;
        }

        boolean isDetected() {
            return this != UNVERSIONED;
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
            return this.version.compareTo(o.version);
        }

        static Version of(Stream<String> lines) {
            final Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                final String line = it.next();
                final Matcher matcher = PATTERN.matcher(line);
                if (matcher.find()) {
                    // GraalVM/Mandrel:
                    final String version = matcher.group("version");
                    final String distro = matcher.group("distro");
                    // JDK:
                    // e.g. JDK 17.0.1, feature: 17, interim: 0 (not used here), update: 1
                    final String jFeatureMatch = matcher.group("jfeature");
                    final int jFeature = jFeatureMatch == null ? // Old GraalVM versions, like 19, didn't report the Java version.
                            11 : Integer.parseInt(jFeatureMatch);
                    final String jUpdateMatch = matcher.group("jupdate");
                    final int jUpdate = jUpdateMatch == null ? // Some JDK dev builds don't report full version string.
                            UNDEFINED : Integer.parseInt(jUpdateMatch);

                    return new Version(
                            line,
                            version,
                            jFeature,
                            jUpdate,
                            isMandrel(distro) ? Distribution.MANDREL : Distribution.ORACLE);
                }
            }

            return UNVERSIONED;
        }

        private static boolean isMandrel(String s) {
            return s != null && s.contains("Mandrel Distribution");
        }

        @Override
        public String toString() {
            return "Version{" +
                    "version=" + version +
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
        ORACLE,
        MANDREL;
    }
}
