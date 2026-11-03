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

    /**
     * Whether to allow the project's version to be newer than the reference artifact's version (default: false).
     * When false, the versions must match exactly.
     * When true, the rule only fails if the project's version is older than the reference artifact's version.
     */
    private boolean allowNewer = false;

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

    public boolean isAllowNewer() {
        return allowNewer;
    }

    public void setAllowNewer(boolean allowNewer) {
        this.allowNewer = allowNewer;
    }

    @Override
    public String toString() {
        return "{" +
                "artifact='" + artifact + '\'' +
                ", failOnNotFound=" + failOnNotFound +
                ", allowNewer=" + allowNewer +
                '}';
    }
}
