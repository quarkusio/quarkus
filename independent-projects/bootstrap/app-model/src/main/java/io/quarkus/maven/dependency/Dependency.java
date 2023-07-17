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

    default boolean isFlagSet(int flag) {
        return (getFlags() & flag) > 0;
    }
}
