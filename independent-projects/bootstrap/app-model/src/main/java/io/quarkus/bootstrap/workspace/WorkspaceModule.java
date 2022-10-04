package io.quarkus.bootstrap.workspace;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathTree;

public interface WorkspaceModule {

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

        WorkspaceModule build();

        default Mutable mutable() {
            return this;
        }
    }
}
