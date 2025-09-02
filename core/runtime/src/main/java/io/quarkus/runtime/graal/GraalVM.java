package io.quarkus.runtime.graal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Implements version parsing from the {@code com.oracle.svm.core.VM} property inspired by
 * {@code org.graalvm.home.impl.DefaultHomeFinder}.
 * This allows Quarkus to determine the GraalVM version used at build time without depending on
 * {@code org.graalvm.polyglot:polyglot}.
 */
public final class GraalVM {
    private static final Logger log = Logger.getLogger(GraalVM.class);

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

        static Version parse(String value) {
            Matcher versionMatcher = VENDOR_VERS_PATTERN.matcher(value);
            if (versionMatcher.find()) {
                String vendor = versionMatcher.group(VENDOR_PREFIX_GROUP);
                if (GRAALVM_CE_VERS_PREFIX.equals(vendor) || ORACLE_GRAALVM_VERS_PREFIX.equals(vendor)) {
                    String version = versionMatcher.group(VERSION_GROUP);
                    String tokens[] = version.split("\\.", 3);
                    String jdkFeature = tokens[0];
                    String jdkVers = jdkFeature;
                    if (tokens.length == 3) {
                        String interim = tokens[1];
                        String update = tokens[2].split("\\+")[0];
                        jdkVers = String.format("%s.%s.%s", jdkFeature, interim, update);
                    }
                    // For JDK 26+ there is no more version mapping use the JDK version
                    String versionMapping = Version.GRAAL_MAPPING.getOrDefault(jdkFeature, version);
                    return new Version(value, versionMapping, jdkVers, Distribution.GRAALVM);
                } else if (LIBERICA_NIK_VERS_PREFIX.equals(vendor)) {
                    return new Version(value, versionMatcher.group(VERSION_GROUP), Distribution.LIBERICA);
                } else if (MANDREL_VERS_PREFIX.equals(vendor)) {
                    return new Version(value, versionMatcher.group(VERSION_GROUP), Distribution.MANDREL);
                }
            }

            log.warnf("Failed to parse GraalVM version from: %s. Defaulting to currently supported version %s ", value,
                    Version.CURRENT);
            return Version.CURRENT;
        }

    }

    public static class Version implements Comparable<Version> {

        public static final Version VERSION_23_0_0 = new Version("GraalVM 23.0.0", "23.0.0", "17", Distribution.GRAALVM);
        public static final Version VERSION_23_1_0 = new Version("GraalVM 23.1.0", "23.1.0", "21", Distribution.GRAALVM);
        public static final Version VERSION_24_2_0 = new Version("GraalVM 24.2.0", "24.2.0", "24", Distribution.GRAALVM);
        public static final Version VERSION_25_0_0 = new Version("GraalVM 25.0.0", "25.0.0", "25", Distribution.GRAALVM);

        // Temporarily work around https://github.com/quarkusio/quarkus/issues/36246,
        // till we have a consensus on how to move forward in
        // https://github.com/quarkusio/quarkus/issues/34161
        protected static final Map<String, String> GRAAL_MAPPING = Map.of(
                "21", "23.1",
                "22", "24.0",
                "23", "24.1",
                "24", "24.2");
        // Mapping of community major.minor pair to the JDK major version based on
        // GRAAL_MAPPING
        private static final Map<String, String> MANDREL_JDK_REV_MAP;

        static {
            Map<String, String> reverseMap = new HashMap<>(GRAAL_MAPPING.size());
            for (Entry<String, String> entry : GRAAL_MAPPING.entrySet()) {
                reverseMap.put(entry.getValue(), entry.getKey());
            }
            MANDREL_JDK_REV_MAP = Collections.unmodifiableMap(reverseMap);
        }

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

        private static final String DEFAULT_JDK_VERSION = "21";
        protected final String fullVersion;
        public final Runtime.Version javaVersion;
        protected final Distribution distribution;
        private int[] versions;
        private String suffix;

        Version(String fullVersion, String version, Distribution distro) {
            this(fullVersion, version,
                    distro == Distribution.MANDREL || distro == Distribution.LIBERICA ? communityJDKvers(version)
                            : DEFAULT_JDK_VERSION,
                    distro);
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

        /*
         * Reconstruct the JDK version from the given GraalVM community version (Mandrel or Liberica)
         */
        private static String communityJDKvers(String communityVersion) {
            try {
                String[] parts = communityVersion.split("\\.", 4);
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                if ((major == 23 && minor > 0) ||
                        major > 23) {
                    String mandrelMajorMinor = String.format("%s.%s", parts[0], parts[1]);
                    // If we don't find a reverse mapping we use a JDK version >= 25, thus
                    // the feature version is the first part of the quadruple.
                    String feature = MANDREL_JDK_REV_MAP.getOrDefault(mandrelMajorMinor, parts[0]);
                    // Heuristic: The update version of Mandrel and the JDK match.
                    // Interim is usually 0 for the JDK version.
                    // Skip trailing zeroes, as they are not supported by java.lang.Runtime.Version.parse.
                    if ("0".equals(parts[2])) {
                        return feature;
                    }
                    return String.format("%s.%s.%s", feature, "0", parts[2]);
                }
            } catch (Throwable e) {
                // fall-through do default
                log.warnf("Failed to parse JDK version from GraalVM version: %s. Defaulting to currently supported version %s ",
                        communityVersion,
                        DEFAULT_JDK_VERSION);
            }
            return DEFAULT_JDK_VERSION;
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
