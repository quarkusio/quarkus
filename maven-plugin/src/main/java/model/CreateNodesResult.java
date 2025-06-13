package model;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Represents the result of CreateNodesV2 function
 */
public class CreateNodesResult {
    private Map<String, ProjectConfiguration> projects = new LinkedHashMap<>();
    private Map<String, Object> externalNodes = new LinkedHashMap<>();
    
    public CreateNodesResult() {}

    // Getters and setters
    public Map<String, ProjectConfiguration> getProjects() { return projects; }
    public void setProjects(Map<String, ProjectConfiguration> projects) { this.projects = projects; }

    public Map<String, Object> getExternalNodes() { return externalNodes; }
    public void setExternalNodes(Map<String, Object> externalNodes) { this.externalNodes = externalNodes; }
    
    public void addProject(String name, ProjectConfiguration project) {
        this.projects.put(name, project);
    }
}