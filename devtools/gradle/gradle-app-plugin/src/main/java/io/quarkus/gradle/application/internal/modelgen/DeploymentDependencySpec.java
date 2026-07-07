package io.quarkus.gradle.application.internal.modelgen;

record DeploymentDependencySpec(String kind, String value,
        String moduleNotation) implements Comparable<DeploymentDependencySpec> {

    private static final String EXTERNAL = "external";
    private static final String INCLUDED_PROJECT = "included-project";
    private static final String PROJECT = "project";
    private static final String SEPARATOR = "\t";

    static DeploymentDependencySpec external(String dependencyNotation) {
        return new DeploymentDependencySpec(EXTERNAL, dependencyNotation, "");
    }

    static DeploymentDependencySpec project(String projectPath, String moduleNotation) {
        return new DeploymentDependencySpec(PROJECT, projectPath, moduleNotation);
    }

    static DeploymentDependencySpec includedProject(String dependencyNotation) {
        return new DeploymentDependencySpec(INCLUDED_PROJECT, dependencyNotation, dependencyNotation);
    }

    boolean external() {
        return EXTERNAL.equals(kind);
    }

    boolean project() {
        return PROJECT.equals(kind);
    }

    boolean includedProject() {
        return INCLUDED_PROJECT.equals(kind);
    }

    String serialize() {
        return kind + SEPARATOR + value + SEPARATOR + moduleNotation;
    }

    static DeploymentDependencySpec deserialize(String value) {
        String[] parts = value.split(SEPARATOR, -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid deployment dependency spec: " + value);
        }
        DeploymentDependencySpec spec = new DeploymentDependencySpec(parts[0], parts[1], parts[2]);
        if (!spec.external() && !spec.project() && !spec.includedProject()) {
            throw new IllegalArgumentException("Unsupported deployment dependency spec kind: " + parts[0]);
        }
        return spec;
    }

    @Override
    public int compareTo(DeploymentDependencySpec other) {
        int kindComparison = kind.compareTo(other.kind);
        if (kindComparison != 0) {
            return kindComparison;
        }
        return value.compareTo(other.value);
    }
}
