package io.quarkus.deployment.pkg.steps;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class GraalVM {
    static final class Version implements Comparable<Version> {
        private static final Pattern PATTERN = Pattern.compile(
                "(GraalVM|native-image)( Version)? ([1-9][0-9]*)\\.([0-9]+)\\.[0-9]+(-dev\\p{XDigit}*)?([^\n$]*)\\s*");

        static final Version UNVERSIONED = new Version("Undefined", -1, -1, Distribution.ORACLE);
        static final Version VERSION_20_3 = new Version("GraalVM 20.3", 20, 3, Distribution.ORACLE);
        static final Version VERSION_21_1 = new Version("GraalVM 21.1", 21, 1, Distribution.ORACLE);
        static final Version VERSION_21_2 = new Version("GraalVM 21.2", 21, 2, Distribution.ORACLE);
        static final Version VERSION_21_3 = new Version("GraalVM 21.3", 21, 3, Distribution.ORACLE);

        static final Version MINIMUM = VERSION_20_3;
        static final Version CURRENT = VERSION_21_2;

        final String fullVersion;
        final int major;
        final int minor;
        final Distribution distribution;

        Version(String fullVersion, int major, int minor, Distribution distro) {
            this.fullVersion = fullVersion;
            this.major = major;
            this.minor = minor;
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
            if (major > o.major) {
                return 1;
            }

            if (major == o.major) {
                if (minor > o.minor) {
                    return 1;
                } else if (minor == o.minor) {
                    return 0;
                }
            }

            return -1;
        }

        static Version of(Stream<String> lines) {
            final Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                final String line = it.next();
                final Matcher matcher = PATTERN.matcher(line);
                if (matcher.find() && matcher.groupCount() >= 3) {
                    final String major = matcher.group(3);
                    final String minor = matcher.group(4);
                    final String distro = matcher.group(6);
                    return new Version(
                            line,
                            Integer.parseInt(major), Integer.parseInt(minor),
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
                    "major=" + major +
                    ", minor=" + minor +
                    ", distribution=" + distribution +
                    '}';
        }
    }

    enum Distribution {
        ORACLE,
        MANDREL;
    }
}
