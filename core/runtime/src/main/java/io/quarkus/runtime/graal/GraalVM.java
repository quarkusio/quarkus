package io.quarkus.runtime.graal;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.logging.Log;

/**
 * Implements version parsing from the {@code com.oracle.svm.core.VM} property inspired by
 * {@code org.graalvm.home.impl.DefaultHomeFinder}.
 * This allows Quarkus to determine the GraalVM version used at build time without depending on
 * {@code org.graalvm.polyglot:polyglot}.
 */
public final class GraalVM {

    static final class VersionParseHelper {

        private static final String VNUM = "(?<VNUM>[1-9][0-9]*(?:\\.(?:0|[1-9][0-9]*))*)";
        private static final String PRE = "(?:-(?<PRE>[a-zA-Z0-9]+))?";
        private static final String BUILD = "\\+(?<BUILD>0|[1-9][0-9]*(?:\\.(?:0|[1-9][0-9]*))*)?";
        private static final String OPT = "(?:-(?<OPT>[-a-zA-Z0-9.]+))?";
        private static final String VSTR_FORMAT = VNUM + "(?:" + PRE + BUILD + ")?" + OPT;

        private static final String GRAALVM_CE_VERS_PREFIX = "GraalVM CE ";
        private static final String LIBERICA_NIK_VERS_PREFIX = "Liberica-NIK-";
        private static final String MANDREL_VERS_PREFIX = "Mandrel-";
        private static final String ORACLE_GRAALVM_VERS_PREFIX = "Oracle GraalVM ";

        private static final String VENDOR_PREFIX_GROUP = "VENDORPREFIX";

        private static final String VENDOR_PREFIX = "(?<" + VENDOR_PREFIX_GROUP + ">" + GRAALVM_CE_VERS_PREFIX + "|"
                + LIBERICA_NIK_VERS_PREFIX + "|" + MANDREL_VERS_PREFIX + "|" + ORACLE_GRAALVM_VERS_PREFIX + ")";
        private static final Pattern VENDOR_VERS_PATTERN = Pattern.compile(VENDOR_PREFIX + VSTR_FORMAT);

        private static final String VERSION_GROUP = "VNUM";

        private static final Version UNKNOWN_VERSION = null;

        static Version parse(String value) {
            Matcher versionMatcher = VENDOR_VERS_PATTERN.matcher(value);
            if (versionMatcher.find()) {
                String vendor = versionMatcher.group(VENDOR_PREFIX_GROUP);
                if (GRAALVM_CE_VERS_PREFIX.equals(vendor) || ORACLE_GRAALVM_VERS_PREFIX.equals(vendor)) {
                    String version = versionMatcher.group(VERSION_GROUP);
                    String jdkFeature = version.split("\\.", 2)[0];
                    return new Version(value, Version.GRAAL_MAPPING.get(jdkFeature), Distribution.GRAALVM);
                } else if (LIBERICA_NIK_VERS_PREFIX.equals(vendor)) {
                    return new Version(value, versionMatcher.group(VERSION_GROUP), Distribution.LIBERICA);
                } else if (MANDREL_VERS_PREFIX.equals(vendor)) {
                    return new Version(value, versionMatcher.group(VERSION_GROUP), Distribution.MANDREL);
                }
            }

            Log.warnf("Failed to parse GraalVM version from: %s. Defaulting to currently supported version %s ", value,
                    Version.CURRENT);
            return Version.CURRENT;
        }

    }

    public static class Version implements Comparable<Version> {

        public static final Version VERSION_23_0_0 = new Version("GraalVM 23.0.0", "23.0.0", "17", Distribution.GRAALVM);
        public static final Version VERSION_23_1_0 = new Version("GraalVM 23.1.0", "23.1.0", "21", Distribution.GRAALVM);

        // Temporarily work around https://github.com/quarkusio/quarkus/issues/36246,
        // till we have a consensus on how to move forward in
        // https://github.com/quarkusio/quarkus/issues/34161
        protected static final Map<String, String> GRAAL_MAPPING = Map.of(
                "21", "23.1",
                "22", "24.0",
                "23", "24.1",
                "24", "24.2",
                "25", "25.0");

        /**
         * The minimum version of GraalVM supported by Quarkus.
         * Versions prior to this are expected to cause major issues.
         */
        public static final Version MINIMUM = VERSION_23_0_0;
        /**
         * The current version of GraalVM supported by Quarkus.
         * This version is the one actively being tested and is expected to give the best experience.
         */
        public static final Version CURRENT = VERSION_23_1_0;
        /**
         * The minimum version of GraalVM officially supported by Quarkus.
         * Versions prior to this are expected to work but are not given the same level of testing or priority.
         */
        public static final Version MINIMUM_SUPPORTED = CURRENT;

        protected final String fullVersion;
        public final Runtime.Version javaVersion;
        protected final Distribution distribution;
        private int[] versions;
        private String suffix;

        Version(String fullVersion, String version, Distribution distro) {
            this(fullVersion, version, "21", distro);
        }

        Version(String fullVersion, String version, String javaVersion, Distribution distro) {
            this(fullVersion, version, Runtime.Version.parse(javaVersion), distro);
        }

        protected Version(String fullVersion, String version, Runtime.Version javaVersion, Distribution distro) {
            this.fullVersion = fullVersion;
            breakdownVersion(version);
            this.javaVersion = javaVersion;
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

        @Override
        public int compareTo(Version o) {
            return compareTo(o.versions);
        }

        public int compareTo(int[] versions) {
            int i = 0;
            for (; i < this.versions.length; i++) {
                if (i >= versions.length) {
                    if (this.versions[i] != 0) {
                        return 1;
                    }
                } else if (this.versions[i] != versions[i]) {
                    return this.versions[i] - versions[i];
                }
            }
            for (; i < versions.length; i++) {
                if (versions[i] != 0) {
                    return -1;
                }
            }
            return 0;
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

        public String getMajorMinorAsString() {
            if (versions.length >= 2) {
                return versions[0] + "." + versions[1];
            }
            return versions[0] + ".0";
        }

        @Override
        public String toString() {
            return "Version{" +
                    "version="
                    + getVersionAsString() +
                    ", fullVersion=" + fullVersion +
                    ", distribution=" + distribution +
                    ", javaVersion=" + javaVersion +
                    '}';
        }

        public static Version getCurrent() {
            String vendorVersion = System.getProperty("org.graalvm.vendorversion");
            return VersionParseHelper.parse(vendorVersion);
        }
    }

    public enum Distribution {
        GRAALVM,
        LIBERICA,
        MANDREL;
    }
}

/*
 * This class is only meant to be used at native image build time
 */
@Delete
@TargetClass(GraalVM.class)
final class Target_io_quarkus_runtime_graal_GraalVM {
}

@Delete
@TargetClass(GraalVM.Distribution.class)
final class Target_io_quarkus_runtime_graal_GraalVM_Distribution {
}

@Delete
@TargetClass(GraalVM.Version.class)
final class Target_io_quarkus_runtime_graal_GraalVM_Version {
}

@Delete
@TargetClass(GraalVM.VersionParseHelper.class)
final class Target_io_quarkus_runtime_graal_GraalVM_VersionParseHelper {
}
