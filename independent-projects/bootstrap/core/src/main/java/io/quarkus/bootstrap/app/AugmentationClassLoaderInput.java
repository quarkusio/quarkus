package io.quarkus.bootstrap.app;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import io.quarkus.bootstrap.classloading.ClassLoaderEventListener;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * All inputs that determine the contents of an augmentation class loader.
 * <p>
 * This record serves two purposes:
 * <ol>
 * <li>It is the explicit parameter list for the static
 * {@link CuratedApplication#buildAugmentationClassLoader} factory method,
 * making all dependencies visible.</li>
 * <li>Its {@link #isCompatibleWith} method is the single source of truth for
 * whether two augmentation class loaders can be shared.</li>
 * </ol>
 */
public record AugmentationClassLoaderInput(
        // Compatibility-affecting fields
        ConfiguredClassLoading configuredClassLoading,
        List<Path> additionalDeploymentArchives,

        // Structural fields
        // Needed for building the CL but always identical across profiles in the
        // same Surefire/Failsafe run. Not compared in isCompatibleWith().
        ClassLoader baseClassLoader,
        boolean isolateDeployment,
        boolean assertionsEnabled,
        List<ClassLoaderEventListener> classLoaderEventListeners,
        Collection<ResolvedDependency> dependencies,

        // Cosmetic fields
        // Affect the CL display name only, not its content.
        QuarkusBootstrap.Mode mode,
        String classLoaderNameSuffix) {

    /**
     * Check whether this input is compatible with another for the purpose of
     * sharing an augmentation class loader.
     * <p>
     * Delegates to {@link ConfiguredClassLoading#isCompatibleForAugmentationWith}
     * for classloading configuration fields, and additionally compares
     * {@code additionalDeploymentArchives}.
     */
    public boolean isCompatibleWith(AugmentationClassLoaderInput other) {
        return configuredClassLoading.isCompatibleForAugmentationWith(other.configuredClassLoading)
                && additionalDeploymentArchives.equals(other.additionalDeploymentArchives);
    }
}
