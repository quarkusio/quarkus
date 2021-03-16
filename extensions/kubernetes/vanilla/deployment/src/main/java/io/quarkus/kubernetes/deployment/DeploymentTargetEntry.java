package io.quarkus.kubernetes.deployment;

public class DeploymentTargetEntry {
    private final String name;
    private final String kind;
    private final int priority;

    public DeploymentTargetEntry(String name, String kind, int priority) {
        this.name = name;
        this.kind = kind;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public int getPriority() {
        return priority;
    }
}
