package io.quarkus.maven.dependency;

import java.util.Collection;
import java.util.List;

public interface Dependency extends ArtifactCoords {

    String SCOPE_COMPILE = "compile";
    String SCOPE_IMPORT = "import";

    public static Dependency of(String groupId, String artifactId) {
        return new ArtifactDependency(groupId, artifactId, null, ArtifactCoords.TYPE_JAR, null);
    }

    public static Dependency of(String groupId, String artifactId, String version) {
        return new ArtifactDependency(groupId, artifactId, null, ArtifactCoords.TYPE_JAR, version);
    }

    public static Dependency pomImport(String groupId, String artifactId, String version) {
        return new ArtifactDependency(groupId, artifactId, null, ArtifactCoords.TYPE_POM, version, SCOPE_IMPORT, false);
    }

    String getScope();

    default Collection<ArtifactKey> getExclusions() {
        return List.of();
    }

    int getFlags();

    default boolean isOptional() {
        return isFlagSet(DependencyFlags.OPTIONAL);
    }

    default boolean isDirect() {
        return isFlagSet(DependencyFlags.DIRECT);
    }

    default boolean isRuntimeExtensionArtifact() {
        return isFlagSet(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
    }

    default boolean isRuntimeCp() {
        return isFlagSet(DependencyFlags.RUNTIME_CP);
    }

    default boolean isDeploymentCp() {
        return isFlagSet(DependencyFlags.DEPLOYMENT_CP);
    }

    default boolean isWorkspaceModule() {
        return isFlagSet(DependencyFlags.WORKSPACE_MODULE);
    }

    default boolean isReloadable() {
        return isFlagSet(DependencyFlags.RELOADABLE) && isWorkspaceModule();
    }

    default boolean isClassLoaderParentFirst() {
        return isFlagSet(DependencyFlags.CLASSLOADER_PARENT_FIRST);
    }

    /**
     * Checks whether a dependency has a given flag set.
     *
     * @param flag flag to check
     * @return true if the flag is set, otherwise false
     */
    default boolean isFlagSet(int flag) {
        return (getFlags() & flag) == flag;
    }

    /**
     * Checks whether any of the flags are set on a dependency
     *
     * @param flags flags to check
     * @return true if any of the flags are set, otherwise false
     */
    default boolean hasAnyFlag(int... flags) {
        for (var flag : flags) {
            if (isFlagSet(flag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether all the passed in flags are set on a dependency
     *
     * @param flags flags to check
     * @return true if all the passed in flags are set on a dependency, otherwise false
     */
    default boolean hasAllFlags(int... flags) {
        for (var flag : flags) {
            if (!isFlagSet(flag)) {
                return false;
            }
        }
        return true;
    }
}
