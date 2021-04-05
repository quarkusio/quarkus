package io.quarkus.deployment.pkg.steps;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class GraalVM {
    static final class Version implements Comparable<Version> {
        private static final Pattern PATTERN = Pattern.compile(
                "GraalVM Version (([1-9][0-9]*)\\.([0-9]+)\\.[0-9]+|\\p{XDigit}*)[^(\n$]*(\\(Mandrel Distribution\\))?\\s*");

        static final Version UNVERSIONED = new Version("Undefined", -1, -1, Distribution.ORACLE);
        static final Version SNAPSHOT_ORACLE = new Version("Snapshot", Integer.MAX_VALUE, Integer.MAX_VALUE,
                Distribution.ORACLE);
        static final Version SNAPSHOT_MANDREL = new Version("Snapshot", Integer.MAX_VALUE, Integer.MAX_VALUE,
                Distribution.MANDREL);

        static final Version VERSION_20_3 = new Version("GraalVM 20.3", 20, 3, Distribution.ORACLE);
        static final Version VERSION_21_0 = new Version("GraalVM 21.0", 21, 0, Distribution.ORACLE);

        static final Version MINIMUM = VERSION_20_3;
        static final Version CURRENT = VERSION_21_0;

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

        boolean isSnapshot() {
            return this == SNAPSHOT_ORACLE || this == SNAPSHOT_MANDREL;
        }

        boolean isNewerThan(Version version) {
            return this.compareTo(version) > 0;
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
                    final String distro = matcher.group(4);
                    if (isSnapshot(matcher.group(2))) {
                        return isMandrel(distro) ? SNAPSHOT_MANDREL : SNAPSHOT_ORACLE;
                    } else {
                        return new Version(
                                line,
                                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                                isMandrel(distro) ? Distribution.MANDREL : Distribution.ORACLE);
                    }
                }
            }

            return UNVERSIONED;
        }

        private static boolean isSnapshot(String s) {
            return s == null;
        }

        private static boolean isMandrel(String s) {
            return "(Mandrel Distribution)".equals(s);
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
