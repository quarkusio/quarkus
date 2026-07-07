package io.quarkus.gradle.model.config;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

/**
 * Lazy artifact views of local component class directories, resource directories, and JARs.
 * <p>
 * The views use variant reselection and lenient resolution so application-model generation can consume producer outputs
 * without reaching into producer projects. This is an implementation-facing Gradle model used to support isolated
 * projects.
 */
public final class LocalComponentOutputViews {

    private final ArtifactView classes;
    private final ArtifactView resources;
    private final ArtifactView jars;

    private LocalComponentOutputViews(ObjectFactory objects, Configuration configuration) {
        classes = artifactView(objects, configuration, LibraryElements.CLASSES,
                ArtifactTypeDefinition.JVM_CLASS_DIRECTORY);
        resources = artifactView(objects, configuration, LibraryElements.RESOURCES,
                ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY);
        jars = artifactView(objects, configuration, LibraryElements.JAR,
                ArtifactTypeDefinition.JAR_TYPE);
    }

    /**
     * Creates all three output views without resolving them.
     *
     * @param objects Gradle object factory used to create the requested library-element attributes
     * @param configuration the resolvable configuration whose component outputs are viewed
     * @return lazy local-output views
     */
    public static LocalComponentOutputViews of(ObjectFactory objects, Configuration configuration) {
        return new LocalComponentOutputViews(objects, configuration);
    }

    /** @return the variant-reselected class-directory artifact view */
    public ArtifactView classes() {
        return classes;
    }

    /** @return the variant-reselected resource-directory artifact view */
    public ArtifactView resources() {
        return resources;
    }

    /** @return the variant-reselected JAR artifact view */
    public ArtifactView jars() {
        return jars;
    }

    /** @return a provider of resolved class-directory artifacts */
    public Provider<Set<ResolvedArtifactResult>> classArtifacts() {
        return classes.getArtifacts().getResolvedArtifacts();
    }

    /** @return a provider of resolved resource-directory artifacts */
    public Provider<Set<ResolvedArtifactResult>> resourceArtifacts() {
        return resources.getArtifacts().getResolvedArtifacts();
    }

    /** @return a provider of resolved JAR artifacts */
    public Provider<Set<ResolvedArtifactResult>> jarArtifacts() {
        return jars.getArtifacts().getResolvedArtifacts();
    }

    /** @return the lazily resolved class-directory files */
    public FileCollection classFiles() {
        return classes.getFiles();
    }

    /** @return the lazily resolved resource-directory files */
    public FileCollection resourceFiles() {
        return resources.getFiles();
    }

    /** @return the lazily resolved JAR files */
    public FileCollection jarFiles() {
        return jars.getFiles();
    }

    /**
     * Returns JARs only for components for which Gradle did not expose class or resource directory variants.
     * <p>
     * The result preserves the encounter order of the JAR artifact view and avoids representing a local component both
     * by its output directories and its archive.
     *
     * @param providers factory used to keep the filtering lazy
     * @return a provider of non-duplicated JAR files
     */
    public Provider<Set<File>> jarFilesWithoutOutputVariants(ProviderFactory providers) {
        return providers.provider(() -> {
            Set<String> componentsWithOutputVariants = new HashSet<>();
            collectComponentIds(classArtifacts().get(), componentsWithOutputVariants);
            collectComponentIds(resourceArtifacts().get(), componentsWithOutputVariants);
            Set<File> jarFiles = new LinkedHashSet<>();
            for (ResolvedArtifactResult artifact : jarArtifacts().get()) {
                if (!componentsWithOutputVariants.contains(componentId(artifact))) {
                    jarFiles.add(artifact.getFile());
                }
            }
            return jarFiles;
        });
    }

    private static ArtifactView artifactView(ObjectFactory objects, Configuration configuration,
            String libraryElements, String artifactType) {
        return configuration.getIncoming().artifactView(view -> {
            view.withVariantReselection();
            view.lenient(true);
            view.attributes(attributes -> {
                attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objects.named(LibraryElements.class, libraryElements));
                attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, artifactType);
            });
        });
    }

    private static void collectComponentIds(Set<ResolvedArtifactResult> artifacts, Set<String> target) {
        for (ResolvedArtifactResult artifact : artifacts) {
            target.add(componentId(artifact));
        }
    }

    private static String componentId(ResolvedArtifactResult artifact) {
        return artifact.getId().getComponentIdentifier().getDisplayName();
    }
}
