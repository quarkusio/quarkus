package model;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a group of targets organized by Maven lifecycle phase
 */
public class TargetGroup {
    private String phase;
    private String description;
    private List<String> targets = new ArrayList<>();
    private int order;
    
    public TargetGroup() {}
    
    public TargetGroup(String phase, String description, int order) {
        this.phase = phase;
        this.description = description;
        this.order = order;
    }

    // Getters and setters
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTargets() { return targets; }
    public void setTargets(List<String> targets) { this.targets = targets; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    
    public void addTarget(String targetName) {
        if (!targets.contains(targetName)) {
            targets.add(targetName);
        }
    }
}