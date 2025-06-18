package model;

import java.util.List;

/**
 * Represents a target dependency in object form for Nx.
 * This allows for more precise control over dependencies than simple strings.
 * 
 * Based on Nx documentation:
 * - dependencies: Run this target on all dependencies first
 * - projects: Run target on specific projects first
 * - target: target name
 * - params: "forward" or "ignore", defaults to "ignore"
 */
public class TargetDependency {
    private Boolean dependencies;
    private List<String> projects;
    private String target;
    private String params = "ignore";
    
    public TargetDependency() {}
    
    public TargetDependency(String target) {
        this.target = target;
    }
    
    public TargetDependency(String target, Boolean dependencies) {
        this.target = target;
        this.dependencies = dependencies;
    }
    
    public TargetDependency(String target, List<String> projects) {
        this.target = target;
        this.projects = projects;
    }
    
    // Getters and setters
    public Boolean getDependencies() { return dependencies; }
    public void setDependencies(Boolean dependencies) { this.dependencies = dependencies; }
    
    public List<String> getProjects() { return projects; }
    public void setProjects(List<String> projects) { this.projects = projects; }
    
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
}