package io.quarkus.maven.dependency;

public interface Dependency extends ArtifactCoords {

    String getScope();

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

    default boolean isWorkspacetModule() {
        return isFlagSet(DependencyFlags.WORKSPACE_MODULE);
    }

    default boolean isReloadable() {
        return isFlagSet(DependencyFlags.RELOADABLE) && isWorkspacetModule();
    }

    default boolean isFlagSet(int flag) {
        return (getFlags() & flag) > 0;
    }
}
