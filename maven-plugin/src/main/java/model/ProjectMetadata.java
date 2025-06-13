package model;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

/**
 * Represents project metadata
 */
public class ProjectMetadata {
    private String groupId;
    private String artifactId;
    private String version;
    private String packaging;
    private Map<String, List<String>> targetGroups = new LinkedHashMap<>();
    
    public ProjectMetadata() {}
    
    public ProjectMetadata(String groupId, String artifactId, String version, String packaging) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
    }

    // Getters and setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getPackaging() { return packaging; }
    public void setPackaging(String packaging) { this.packaging = packaging; }

    public Map<String, List<String>> getTargetGroups() { return targetGroups; }
    public void setTargetGroups(Map<String, List<String>> targetGroups) { this.targetGroups = targetGroups; }
    
    public void addTargetGroup(String groupName, List<String> targets) {
        this.targetGroups.put(groupName, targets);
    }
}