package io.quarkus.deployment.pkg.steps;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class GraalVM {
    static final class Version implements Comparable<Version> {
        private static final Pattern PATTERN = Pattern.compile(
                "(GraalVM|native-image)( Version)? (?<version>[1-9][0-9]*(\\.[0-9]+)+(-dev\\p{XDigit}*)?)(?<distro>[^\n$]*)(Java Version (?<javaVersion>[^)]+))?\\s*");

        static final Version UNVERSIONED = new Version("Undefined", "snapshot", Distribution.ORACLE);
        static final Version VERSION_21_2 = new Version("GraalVM 21.2", "21.2", Distribution.ORACLE);
        static final Version VERSION_21_3 = new Version("GraalVM 21.3", "21.3", Distribution.ORACLE);
        static final Version VERSION_21_3_0 = new Version("GraalVM 21.3.0", "21.3.0", Distribution.ORACLE);

        static final Version MINIMUM = VERSION_21_2;
        static final Version CURRENT = VERSION_21_3;

        final String fullVersion;
        final org.graalvm.home.Version version;
        final int javaVersion;
        final Distribution distribution;

        Version(String fullVersion, String version, Distribution distro) {
            this(fullVersion, version, 11, distro);
        }

        Version(String fullVersion, String version, int javaVersion, Distribution distro) {
            this.fullVersion = fullVersion;
            this.version = org.graalvm.home.Version.parse(version);
            this.javaVersion = javaVersion;
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
                    final String version = matcher.group("version");
                    final String distro = matcher.group("distro");
                    String javaVersionMatch = matcher.group("javaVersion");
                    final int javaVersion = javaVersionMatch == null ? // Old GraalVM versions, like 19, didn't report the Java version
                            11 : Integer.parseInt(javaVersionMatch.split("\\.")[0]);
                    return new Version(
                            line,
                            version,
                            javaVersion,
                            isMandrel(distro) ? Distribution.MANDREL : Distribution.ORACLE);
                }
            }

            return UNVERSIONED;
        }

        private static boolean isMandrel(String s) {
            if (s == null) {
                return false;
            }
            return s.contains("Mandrel Distribution");
        }

        @Override
        public String toString() {
            return "Version{" +
                    "version=" + version +
                    ", distribution=" + distribution +
                    ", javaVersion=" + javaVersion +
                    '}';
        }

        public boolean isJava17() {
            return javaVersion == 17;
        }
    }

    enum Distribution {
        ORACLE,
        MANDREL;
    }
}
