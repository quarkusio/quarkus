package model;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Represents an Nx project configuration
 */
public class ProjectConfiguration {
    private String name;
    private String root;
    private String sourceRoot;
    private String projectType;
    private Map<String, TargetConfiguration> targets = new LinkedHashMap<>();
    private ProjectMetadata metadata;
    
    public ProjectConfiguration() {}
    
    public ProjectConfiguration(String root) {
        this.root = root;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }

    public String getSourceRoot() { return sourceRoot; }
    public void setSourceRoot(String sourceRoot) { this.sourceRoot = sourceRoot; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public Map<String, TargetConfiguration> getTargets() { return targets; }
    public void setTargets(Map<String, TargetConfiguration> targets) { this.targets = targets; }

    public ProjectMetadata getMetadata() { return metadata; }
    public void setMetadata(ProjectMetadata metadata) { this.metadata = metadata; }
}