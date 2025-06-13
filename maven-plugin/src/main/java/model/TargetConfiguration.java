package model;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Represents an Nx target configuration
 */
public class TargetConfiguration {
    private String executor;
    private Map<String, Object> options = new LinkedHashMap<>();
    private List<String> inputs = new ArrayList<>();
    private List<String> outputs = new ArrayList<>();
    private List<String> dependsOn = new ArrayList<>();
    private TargetMetadata metadata;
    
    public TargetConfiguration() {}
    
    public TargetConfiguration(String executor) {
        this.executor = executor;
    }

    // Getters and setters
    public String getExecutor() { return executor; }
    public void setExecutor(String executor) { this.executor = executor; }

    public Map<String, Object> getOptions() { return options; }
    public void setOptions(Map<String, Object> options) { this.options = options; }

    public List<String> getInputs() { return inputs; }
    public void setInputs(List<String> inputs) { this.inputs = inputs; }

    public List<String> getOutputs() { return outputs; }
    public void setOutputs(List<String> outputs) { this.outputs = outputs; }

    public List<String> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }

    public TargetMetadata getMetadata() { return metadata; }
    public void setMetadata(TargetMetadata metadata) { this.metadata = metadata; }
}