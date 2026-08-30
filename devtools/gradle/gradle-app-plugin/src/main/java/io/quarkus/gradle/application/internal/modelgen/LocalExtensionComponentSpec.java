package io.quarkus.gradle.application.internal.modelgen;

record LocalExtensionComponentSpec(String buildPath, String projectPath, String moduleNotation)
        implements
            Comparable<LocalExtensionComponentSpec> {

    private static final String SEPARATOR = "\t";

    boolean sameBuild(String currentBuildPath) {
        return buildPath.equals(currentBuildPath);
    }

    String serialize() {
        return buildPath + SEPARATOR + projectPath + SEPARATOR + moduleNotation;
    }

    static LocalExtensionComponentSpec deserialize(String value) {
        String[] parts = value.split(SEPARATOR, -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid local extension component spec: " + value);
        }
        return new LocalExtensionComponentSpec(parts[0], parts[1], parts[2]);
    }

    @Override
    public int compareTo(LocalExtensionComponentSpec other) {
        int buildComparison = buildPath.compareTo(other.buildPath);
        return buildComparison != 0 ? buildComparison : projectPath.compareTo(other.projectPath);
    }
}
