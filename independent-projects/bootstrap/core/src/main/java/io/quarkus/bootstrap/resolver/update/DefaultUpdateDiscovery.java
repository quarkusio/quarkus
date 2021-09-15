package io.quarkus.bootstrap.resolver.update;

import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.maven.dependency.ResolvedDependency;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultUpdateDiscovery implements UpdateDiscovery {

    private final AppModelResolver resolver;
    private final VersionUpdateNumber updateNumber;

    public DefaultUpdateDiscovery(AppModelResolver resolver, VersionUpdateNumber updateNumber) {
        this.resolver = resolver;
        this.updateNumber = updateNumber;
    }

    @Override
    public List<String> listUpdates(ResolvedDependency artifact) {
        try {
            return resolver.listLaterVersions(artifact, resolveUpToVersion(artifact), false);
        } catch (AppModelResolverException e) {
            throw new RuntimeException("Failed to collect later versions", e);
        }
    }

    @Override
    public String getNextVersion(ResolvedDependency artifact) {
        try {
            return resolver.getNextVersion(artifact, getFromVersion(artifact), true, resolveUpToVersion(artifact), false);
        } catch (AppModelResolverException e) {
            throw new RuntimeException("Failed to determine the next available version", e);
        }
    }

    @Override
    public String getLatestVersion(ResolvedDependency artifact) {
        /*
         * to control how the versions are compared
         * DefaultArtifactVersion latest = null;
         * String latestStr = null;
         * for (String version : listUpdates(artifact)) {
         * final DefaultArtifactVersion next = new DefaultArtifactVersion(version);
         * if (latest == null || next.compareTo(latest) > 0) {
         * latest = next;
         * latestStr = version;
         * }
         * }
         * return latestStr;
         */
        try {
            return resolver.getLatestVersion(artifact, resolveUpToVersion(artifact), false);
        } catch (AppModelResolverException e) {
            throw new RuntimeException("Failed to determine the latest available version", e);
        }
    }

    private String resolveUpToVersion(ResolvedDependency artifact) {
        if (updateNumber == VersionUpdateNumber.MAJOR) {
            return null;
        }

        // here we are looking for the major version which is going to be used
        // as the base for the version range to look for the updates
        final String version = artifact.getVersion();
        final int majorMinorSep = version.indexOf('.');
        if (majorMinorSep <= 0) {
            throw new RuntimeException("Failed to determine the major version in " + version);
        }
        final String majorStr = version.substring(0, majorMinorSep);
        if (updateNumber == VersionUpdateNumber.MINOR) {
            final long major;
            try {
                major = Long.parseLong(majorStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        "The version is expected to start with a number indicating the major version: " + version);
            }
            return String.valueOf(major + 1) + ".alpha";
        }

        final int minorMicroSep = version.indexOf('.', majorMinorSep + 1);
        if (minorMicroSep <= 0) {
            throw new RuntimeException("Failed to determine the minor version in " + version);
        }
        final String minorStr = version.substring(majorMinorSep + 1, minorMicroSep);
        final long minor;
        try {
            minor = Long.parseLong(minorStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "Failed to parse the minor number in version: " + version);
        }
        return majorStr + "." + String.valueOf(minor + 1) + ".alpha";
    }

    private String getFromVersion(ResolvedDependency artifact) {
        // here we are looking for the major version which is going to be used
        // as the base for the version range to look for the updates
        final String version = artifact.getVersion();
        final int majorMinorSep = version.indexOf('.');
        if (majorMinorSep <= 0) {
            throw new RuntimeException("Failed to determine the major version in " + version);
        }
        final String majorStr = version.substring(0, majorMinorSep);
        if (updateNumber == VersionUpdateNumber.MAJOR) {
            final long major;
            try {
                major = Long.parseLong(majorStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        "The version is expected to start with a number indicating the major version: " + version);
            }
            return String.valueOf(major + 1) + ".alpha";
        }

        final int minorMicroSep = version.indexOf('.', majorMinorSep + 1);
        if (minorMicroSep <= 0) {
            throw new RuntimeException("Failed to determine the minor version in " + version);
        }
        final String minorStr = version.substring(majorMinorSep + 1, minorMicroSep);
        if (updateNumber == VersionUpdateNumber.MINOR) {
            final long minor;
            try {
                minor = Long.parseLong(minorStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        "Failed to parse the minor number in version: " + version);
            }
            return majorStr + "." + String.valueOf(minor + 1) + ".alpha";
        }

        if (minorMicroSep == version.length() - 1) {
            throw new RuntimeException("Failed to determine the micro version in " + version);
        }
        final String microStr = version.substring(minorMicroSep + 1);

        final long micro;
        try {
            micro = Long.parseLong(microStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "Failed to parse the micro number in version: " + version);
        }
        return majorStr + "." + minorStr + "." + String.valueOf(micro + 1) + ".alpha";
    }
}
