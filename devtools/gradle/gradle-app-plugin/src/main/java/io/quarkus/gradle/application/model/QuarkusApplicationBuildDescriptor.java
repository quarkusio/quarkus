package io.quarkus.gradle.application.model;

/**
 * Identifies one named application build and its package kind.
 * <p>
 * Build descriptors are immutable values used when deriving task names and
 * selecting build-specific behavior.
 *
 * @param name the non-blank name of the build
 * @param type the package kind produced by the build
 */
public record QuarkusApplicationBuildDescriptor(String name, QuarkusApplicationBuildType type) {

    /**
     * Creates and validates a build descriptor.
     */
    public QuarkusApplicationBuildDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Quarkus application build descriptor requires a name");
        }
        if (type == null) {
            throw new IllegalArgumentException("Quarkus application build descriptor requires a type");
        }
    }

    /**
     * Creates a validated descriptor.
     *
     * @param name the non-blank build name
     * @param type the build type
     * @return the descriptor
     */
    public static QuarkusApplicationBuildDescriptor of(String name, QuarkusApplicationBuildType type) {
        return new QuarkusApplicationBuildDescriptor(name, type);
    }
}
