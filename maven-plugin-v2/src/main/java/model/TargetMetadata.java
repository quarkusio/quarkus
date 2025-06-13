package model;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents target metadata
 */
public class TargetMetadata {
    private String type;
    private String phase;
    private String plugin;
    private String goal;
    private String executionId;
    private List<String> technologies = new ArrayList<>();
    private String description;
    
    public TargetMetadata() {}
    
    public TargetMetadata(String type, String description) {
        this.type = type;
        this.description = description;
    }

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getPlugin() { return plugin; }
    public void setPlugin(String plugin) { this.plugin = plugin; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}