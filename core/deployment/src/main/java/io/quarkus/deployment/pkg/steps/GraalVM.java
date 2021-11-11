package io.quarkus.deployment.pkg.steps;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class GraalVM {
    static final class Version implements Comparable<Version> {
        private static final Pattern PATTERN = Pattern.compile(
                "(GraalVM|native-image)( Version)? ([1-9][0-9]*(\\.([0-9]+))+(-dev\\p{XDigit}*)?)([^\n$]*)\\s*");

        static final Version UNVERSIONED = new Version("Undefined", "snapshot", Distribution.ORACLE);
        static final Version VERSION_21_2 = new Version("GraalVM 21.2", "21.2", Distribution.ORACLE);
        static final Version VERSION_21_3 = new Version("GraalVM 21.3", "21.3", Distribution.ORACLE);

        static final Version MINIMUM = VERSION_21_2;
        static final Version CURRENT = VERSION_21_3;

        final String fullVersion;
        final org.graalvm.home.Version version;
        final Distribution distribution;

        Version(String fullVersion, String version, Distribution distro) {
            this.fullVersion = fullVersion;
            this.version = org.graalvm.home.Version.parse(version);
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

        @Override
        public int compareTo(Version o) {
            return this.version.compareTo(o.version);
        }

        static Version of(Stream<String> lines) {
            final Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                final String line = it.next();
                final Matcher matcher = PATTERN.matcher(line);
                if (matcher.find() && matcher.groupCount() >= 3) {
                    final String version = matcher.group(3);
                    final String distro = matcher.group(7);
                    return new Version(
                            line,
                            version,
                            isMandrel(distro) ? Distribution.MANDREL : Distribution.ORACLE);
                }
            }

            return UNVERSIONED;
        }

        private static boolean isMandrel(String s) {
            return s.contains("Mandrel Distribution");
        }

        @Override
        public String toString() {
            return "Version{" +
                    "version=" + version +
                    ", distribution=" + distribution +
                    '}';
        }
    }

    enum Distribution {
        ORACLE,
        MANDREL;
    }
}
