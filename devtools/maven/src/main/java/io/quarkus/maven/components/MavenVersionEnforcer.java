package io.quarkus.maven.components;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = MavenVersionEnforcer.class, instantiationStrategy = "per-lookup")
public class MavenVersionEnforcer {

    public void ensureMavenVersion(Log log, MavenSession session) throws MojoExecutionException {
        final String supported;
        try {
            supported = getSupportedMavenVersions();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to ensure Quarkus Maven version compatibility", e);
        }
        String mavenVersion = session.getSystemProperties().getProperty("maven.version");
        if (log.isDebugEnabled()) {
            log.debug("Detected Maven Version: " + mavenVersion);
        }
        DefaultArtifactVersion detectedVersion = new DefaultArtifactVersion(mavenVersion);
        enforce(log, supported, detectedVersion);
    }

    private static String getSupportedMavenVersions() throws IOException {
        return loadQuarkusProperties().getProperty("supported-maven-versions");
    }

    private static Properties loadQuarkusProperties() throws IOException {
        final String resource = "quarkus.properties";
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (is == null) {
            throw new IOException("Could not locate " + resource + " on the classpath");
        }
        final Properties props = new Properties();
        try {
            props.load(is);
        } catch (IOException e) {
            throw new IOException("Failed to load " + resource + " from the classpath", e);
        }
        return props;
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
        }
        if (!actualMavenVersion.toString().equals(requiredMavenVersionRange)) {
            try {
                final VersionRange vr = VersionRange.createFromVersionSpec(requiredMavenVersionRange);
                if (!containsVersion(vr, actualMavenVersion)) {
                    throw new MojoExecutionException(getDetectedVersionStr(actualMavenVersion.toString())
                            + " is not supported, it must be in " + vr + ".");
                }
            } catch (InvalidVersionSpecificationException e) {
                throw new MojoExecutionException("The requested Maven version "
                        + requiredMavenVersionRange + " is invalid.", e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(
                    getDetectedVersionStr(actualMavenVersion.toString()) + " is allowed in " + requiredMavenVersionRange + ".");
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

    private static String getDetectedVersionStr(String version) {
        return "Detected Maven Version (" + version + ") ";
    }
}
