package io.quarkus.gradle.dependency;

import java.util.Set;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExcludeRule;

class ManualPlatformSpec {
    private final Set<Constraint> constraints;
    private final Set<ExcludeRule> exclusions;

    public ManualPlatformSpec(Set<Constraint> constraints, Set<ExcludeRule> exclusions) {
        this.constraints = constraints;
        this.exclusions = exclusions;
    }

    public Set<Constraint> getConstraints() {
        return constraints;
    }

    public Set<ExcludeRule> getExclusions() {
        return exclusions;
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

        public boolean matches(Dependency dependency) {
            return groupId.equals(dependency.getGroup())
                    && artifactId.equals(dependency.getName());
        }
    }
}