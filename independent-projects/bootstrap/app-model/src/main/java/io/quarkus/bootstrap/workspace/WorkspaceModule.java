package io.quarkus.bootstrap.workspace;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.Mappable;
import io.quarkus.bootstrap.model.MappableCollectionFactory;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathTree;

public interface WorkspaceModule extends Mappable {

    static Mutable builder() {
        return DefaultWorkspaceModule.builder();
    }

    WorkspaceModuleId getId();

    File getModuleDir();

    File getBuildDir();

    Collection<String> getSourceClassifiers();

    boolean hasSources(String classifier);

    ArtifactSources getSources(String classifier);

    default boolean hasMainSources() {
        return hasSources(ArtifactSources.MAIN);
    }

    default boolean hasTestSources() {
        return hasSources(ArtifactSources.TEST);
    }

    default ArtifactSources getMainSources() {
        return getSources(ArtifactSources.MAIN);
    }

    default ArtifactSources getTestSources() {
        return getSources(ArtifactSources.TEST);
    }

    PathCollection getBuildFiles();

    default PathTree getContentTree(String classifier) {
        final ArtifactSources artifactSources = getSources(classifier);
        return artifactSources == null || !artifactSources.isOutputAvailable() ? EmptyPathTree.getInstance()
                : artifactSources.getOutputTree();
    }

    Collection<Dependency> getDirectDependencyConstraints();

    Collection<Dependency> getDirectDependencies();

    Collection<String> getTestClasspathDependencyExclusions();

    Collection<String> getAdditionalTestClasspathElements();

    WorkspaceModule getParent();

    Mutable mutable();

    @Override
    default Map<String, Object> asMap(MappableCollectionFactory factory) {
        final Map<String, Object> map = factory.newMap();
        map.put(BootstrapConstants.MAPPABLE_MODULE_ID, getId().toString());
        if (getModuleDir() != null) {
            map.put(BootstrapConstants.MAPPABLE_MODULE_DIR, getModuleDir().toString());
        }
        if (getBuildDir() != null) {
            map.put(BootstrapConstants.MAPPABLE_BUILD_DIR, getBuildDir().toString());
        }
        if (!getBuildFiles().isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_BUILD_FILES, Mappable.iterableToStringCollection(getBuildFiles(), factory));
        }
        var classifiers = getSourceClassifiers();
        if (!classifiers.isEmpty()) {
            final Collection<Object> artifactSources = factory.newCollection(classifiers.size());
            for (String classifier : classifiers) {
                artifactSources.add(getSources(classifier).asMap(factory));
            }
            map.put(BootstrapConstants.MAPPABLE_ARTIFACT_SOURCES, artifactSources);
        }
        if (getParent() != null) {
            map.put(BootstrapConstants.MAPPABLE_PARENT, getParent().getId().toString());
        }
        if (!getTestClasspathDependencyExclusions().isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_TEST_CP_DEPENDENCY_EXCLUSIONS,
                    Mappable.toStringCollection(getTestClasspathDependencyExclusions(), factory));
        }
        if (!getAdditionalTestClasspathElements().isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_TEST_ADDITIONAL_CP_ELEMENTS,
                    Mappable.toStringCollection(getAdditionalTestClasspathElements(), factory));
        }
        if (!getDirectDependencyConstraints().isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_DIRECT_DEP_CONSTRAINTS,
                    Mappable.asMaps(getDirectDependencyConstraints(), factory));
        }
        if (!getDirectDependencies().isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_DIRECT_DEPS, Mappable.asMaps(getDirectDependencies(), factory));
        }
        return map;
    }

    interface Mutable extends WorkspaceModule {

        Mutable setModuleId(WorkspaceModuleId moduleId);

        Mutable setModuleDir(Path moduleDir);

        Mutable setBuildDir(Path buildDir);

        Mutable setBuildFile(Path buildFile);

        Mutable addDependencyConstraint(Dependency constraint);

        Mutable setDependencyConstraints(List<Dependency> constraints);

        Mutable addDependency(Dependency dep);

        Mutable setDependencies(List<Dependency> deps);

        Mutable addArtifactSources(ArtifactSources sources);

        Mutable setTestClasspathDependencyExclusions(Collection<String> excludes);

        Mutable setAdditionalTestClasspathElements(Collection<String> elements);

        Mutable setParent(WorkspaceModule parent);

        Mutable fromMap(Map<String, Object> moduleMap);

        WorkspaceModule build();

        default Mutable mutable() {
            return this;
        }
    }
}
