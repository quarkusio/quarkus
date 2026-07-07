package io.quarkus.gradle.application.internal.image;

public record ImageReferenceOwner(String projectPath, String buildName, Flavor flavor)
        implements
            Comparable<ImageReferenceOwner> {

    public ImageReferenceOwner {
        projectPath = requireText(projectPath, "project path");
        buildName = requireText(buildName, "build name");
        if (flavor == null) {
            throw new IllegalArgumentException("Container image owner requires a flavor");
        }
    }

    boolean sameLogicalOwner(ImageReferenceOwner other) {
        return projectPath.equals(other.projectPath)
                && buildName.equals(other.buildName)
                && flavor == other.flavor;
    }

    String displayName() {
        return "project '" + projectPath + "', named build '" + buildName + "', "
                + flavor.displayName() + " image";
    }

    @Override
    public int compareTo(ImageReferenceOwner other) {
        int result = projectPath.compareTo(other.projectPath);
        if (result == 0) {
            result = buildName.compareTo(other.buildName);
        }
        if (result == 0) {
            result = flavor.compareTo(other.flavor);
        }
        return result;
    }

    public enum Flavor {
        NORMAL("normal"),
        STARTUP_OPTIMIZED("startup-optimized");

        private final String displayName;

        Flavor(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }
    }

    private static String requireText(String value, String description) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Container image owner " + description + " must not be empty");
        }
        return value;
    }
}
