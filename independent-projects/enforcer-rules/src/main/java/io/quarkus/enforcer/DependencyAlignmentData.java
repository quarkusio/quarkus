package io.quarkus.enforcer;

/**
 * Configuration data for a single dependency that needs to be aligned with a reference artifact.
 */
public class DependencyAlignmentData {

    /**
     * The artifact coordinates in the form "groupId:artifactId" (e.g., "jakarta.persistence:jakarta.persistence-api").
     */
    private String artifact;

    /**
     * Whether to fail if the artifact is not found in the project's dependencyManagement or dependencies (default: true).
     * Note: If the artifact is not found in the reference artifact, the rule will always fail regardless of this setting.
     */
    private boolean failOnNotFound = true;

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public boolean isFailOnNotFound() {
        return failOnNotFound;
    }

    public void setFailOnNotFound(boolean failOnNotFound) {
        this.failOnNotFound = failOnNotFound;
    }

    @Override
    public String toString() {
        return "{" +
                "artifact='" + artifact + '\'' +
                ", failOnNotFound=" + failOnNotFound +
                '}';
    }
}
