import java.util.ArrayList;
import java.util.List;

/**
 * Data class representing the behavior and requirements of a Maven goal.
 * Used to replace hardcoded goal classification logic with dynamic analysis.
 */
public class GoalBehavior {
    private boolean processesSources = false;
    private boolean testRelated = false;
    private boolean needsResources = false;
    private List<String> sourcePaths = new ArrayList<>();
    private List<String> resourcePaths = new ArrayList<>();
    private List<String> outputPaths = new ArrayList<>();
    
    public boolean processesSources() {
        return processesSources;
    }
    
    public void setProcessesSources(boolean processesSources) {
        this.processesSources = processesSources;
    }
    
    public boolean isTestRelated() {
        return testRelated;
    }
    
    public void setTestRelated(boolean testRelated) {
        this.testRelated = testRelated;
    }
    
    public boolean needsResources() {
        return needsResources;
    }
    
    public void setNeedsResources(boolean needsResources) {
        this.needsResources = needsResources;
    }
    
    public List<String> getSourcePaths() {
        return sourcePaths;
    }
    
    public void setSourcePaths(List<String> sourcePaths) {
        this.sourcePaths = new ArrayList<>(sourcePaths);
    }
    
    public List<String> getResourcePaths() {
        return resourcePaths;
    }
    
    public void setResourcePaths(List<String> resourcePaths) {
        this.resourcePaths = new ArrayList<>(resourcePaths);
    }
    
    public List<String> getOutputPaths() {
        return outputPaths;
    }
    
    public void setOutputPaths(List<String> outputPaths) {
        this.outputPaths = new ArrayList<>(outputPaths);
    }
    
    /**
     * Merge this behavior with another, taking the union of all capabilities.
     */
    public GoalBehavior merge(GoalBehavior other) {
        GoalBehavior merged = new GoalBehavior();
        merged.processesSources = this.processesSources || other.processesSources;
        merged.testRelated = this.testRelated || other.testRelated;
        merged.needsResources = this.needsResources || other.needsResources;
        
        merged.sourcePaths.addAll(this.sourcePaths);
        merged.sourcePaths.addAll(other.sourcePaths);
        
        merged.resourcePaths.addAll(this.resourcePaths);
        merged.resourcePaths.addAll(other.resourcePaths);
        
        merged.outputPaths.addAll(this.outputPaths);
        merged.outputPaths.addAll(other.outputPaths);
        
        return merged;
    }
    
    /**
     * Check if this behavior has any defined capabilities.
     */
    public boolean hasAnyBehavior() {
        return processesSources || testRelated || needsResources || 
               !sourcePaths.isEmpty() || !resourcePaths.isEmpty() || !outputPaths.isEmpty();
    }
}