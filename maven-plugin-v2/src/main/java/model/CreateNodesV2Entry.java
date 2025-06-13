package model;

/**
 * Represents a single entry in CreateNodesV2 results array
 * [pomFilePath, CreateNodesResult]
 */
public class CreateNodesV2Entry {
    private String pomFilePath;
    private CreateNodesResult result;
    
    public CreateNodesV2Entry() {}
    
    public CreateNodesV2Entry(String pomFilePath, CreateNodesResult result) {
        this.pomFilePath = pomFilePath;
        this.result = result;
    }

    // Getters and setters
    public String getPomFilePath() { return pomFilePath; }
    public void setPomFilePath(String pomFilePath) { this.pomFilePath = pomFilePath; }

    public CreateNodesResult getResult() { return result; }
    public void setResult(CreateNodesResult result) { this.result = result; }
    
    /**
     * Convert to Object array format expected by Gson
     */
    public Object[] toArray() {
        return new Object[]{pomFilePath, result};
    }
}