package model;

/**
 * Represents a project graph dependency for Nx
 */
public class RawProjectGraphDependency {
    private String source;
    private String target;
    private DependencyType type;
    private String sourceFile;
    
    public enum DependencyType {
        STATIC("static"),
        DYNAMIC("dynamic"), 
        IMPLICIT("implicit");
        
        private final String value;
        
        DependencyType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    public RawProjectGraphDependency() {}
    
    public RawProjectGraphDependency(String source, String target, DependencyType type) {
        this.source = source;
        this.target = target;
        this.type = type;
    }
    
    public RawProjectGraphDependency(String source, String target, DependencyType type, String sourceFile) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.sourceFile = sourceFile;
    }

    // Getters and setters
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public DependencyType getType() { return type; }
    public void setType(DependencyType type) { this.type = type; }

    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
}