package model;

import java.util.List;
import java.util.ArrayList;

/**
 * Top-level result containing both CreateNodesV2 results and CreateDependencies results
 */
public class NxAnalysisResult {
    private List<CreateNodesV2Entry> createNodesResults = new ArrayList<>();
    private List<RawProjectGraphDependency> createDependencies = new ArrayList<>();
    
    public NxAnalysisResult() {}

    // Getters and setters
    public List<CreateNodesV2Entry> getCreateNodesResults() { return createNodesResults; }
    public void setCreateNodesResults(List<CreateNodesV2Entry> createNodesResults) { 
        this.createNodesResults = createNodesResults; 
    }

    public List<RawProjectGraphDependency> getCreateDependencies() { return createDependencies; }
    public void setCreateDependencies(List<RawProjectGraphDependency> createDependencies) { 
        this.createDependencies = createDependencies; 
    }
    
    public void addCreateNodesResult(CreateNodesV2Entry entry) {
        this.createNodesResults.add(entry);
    }
    
    public void addDependency(RawProjectGraphDependency dependency) {
        this.createDependencies.add(dependency);
    }
}