package io.quarkus.maven.components;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;

import io.quarkus.maven.utilities.MojoUtils;

@Component(role = MavenVersionEnforcer.class, instantiationStrategy = "per-lookup")
public class MavenVersionEnforcer {

    public void ensureMavenVersion(Log log, MavenSession session) throws MojoExecutionException {
        String supported = MojoUtils.get("supported-maven-versions");
        String mavenVersion = session.getSystemProperties().getProperty("maven.version");
        log.debug("Detected Maven Version: " + mavenVersion);
        DefaultArtifactVersion detectedVersion = new DefaultArtifactVersion(mavenVersion);
        enforce(log, supported, detectedVersion);
    }

    /**
     * Compares the specified Maven version to see if it is allowed by the defined version range.
     *
     * @param log the log
     * @param requiredMavenVersionRange range of allowed versions for Maven.
     * @param actualMavenVersion the version to be checked.
     * @throws MojoExecutionException the given version fails the enforcement rule
     */
    private void enforce(Log log,
            String requiredMavenVersionRange, ArtifactVersion actualMavenVersion)
            throws MojoExecutionException {
        if (StringUtils.isBlank(requiredMavenVersionRange)) {
            throw new MojoExecutionException("Maven version can't be empty.");
        } else {
            VersionRange vr;
            String msg = "Detected Maven Version (" + actualMavenVersion + ") ";

            if (actualMavenVersion.toString().equals(requiredMavenVersionRange)) {
                log.debug(msg + " is allowed in " + requiredMavenVersionRange + ".");
            } else {
                try {
                    vr = VersionRange.createFromVersionSpec(requiredMavenVersionRange);
                    if (containsVersion(vr, actualMavenVersion)) {
                        log.debug(msg + " is allowed in " + requiredMavenVersionRange + ".");
                    } else {
                        String message = msg + " is not supported, it must be in " + vr + ".";
                        throw new MojoExecutionException(message);
                    }
                } catch (InvalidVersionSpecificationException e) {
                    throw new MojoExecutionException("The requested Maven version "
                            + requiredMavenVersionRange + " is invalid.", e);
                }
            }
        }
    }

    /**
     * Copied from Artifact.VersionRange. This is tweaked to handle singular ranges properly. Currently the default
     * containsVersion method assumes a singular version means allow everything.
     *
     * @param allowedRange range of allowed versions.
     * @param theVersion the version to be checked.
     * @return {@code true} if the version is contained by the range.
     */
    private static boolean containsVersion(VersionRange allowedRange, ArtifactVersion theVersion) {
        boolean matched = false;
        ArtifactVersion recommendedVersion = allowedRange.getRecommendedVersion();
        if (recommendedVersion == null) {
            List<Restriction> restrictions = allowedRange.getRestrictions();
            for (Restriction restriction : restrictions) {
                if (restriction.containsVersion(theVersion)) {
                    matched = true;
                    break;
                }
            }
        } else {
            // only singular versions ever have a recommendedVersion
            int compareTo = recommendedVersion.compareTo(theVersion);
            matched = (compareTo <= 0);
        }
        return matched;
    }
}
