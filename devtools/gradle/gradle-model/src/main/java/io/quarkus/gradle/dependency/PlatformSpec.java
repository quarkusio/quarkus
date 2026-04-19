package io.quarkus.gradle.dependency;

import java.util.Map;
import java.util.Set;

import org.gradle.api.artifacts.ExcludeRule;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

class PlatformSpec {
    private final Map<ArtifactKey, Constraint> constraints;
    private final Set<ExcludeRule> exclusions;
    private final Map<String, ArtifactCoords> defaultCapabilityProviders;

    /**
     * @param constraints platform version constraints
     * @param exclusions platform exclusion rules
     * @param defaultCapabilityProviders mapping from capability name to default provider artifact coordinates
     */
    public PlatformSpec(Map<ArtifactKey, Constraint> constraints, Set<ExcludeRule> exclusions,
            Map<String, ArtifactCoords> defaultCapabilityProviders) {
        this.constraints = constraints;
        this.exclusions = exclusions;
        this.defaultCapabilityProviders = defaultCapabilityProviders;
    }

    public Map<ArtifactKey, Constraint> getConstraints() {
        return constraints;
    }

    public Set<ExcludeRule> getExclusions() {
        return exclusions;
    }

    /**
     * Returns the default capability provider mappings from platform properties.
     * Each entry maps a capability name to the artifact coordinates of the extension
     * that should provide it when no explicit provider exists.
     *
     * @return mapping from capability name to provider artifact coordinates, never null
     */
    public Map<String, ArtifactCoords> getDefaultCapabilityProviders() {
        return defaultCapabilityProviders;
    }

    static class Constraint {
        private final String groupId;
        private final String artifactId;
        private final String version;

        public Constraint(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }
    }
}