package io.quarkus.bootstrap.workspace;

import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathTree;
import java.io.File;
import java.util.Collection;

public interface WorkspaceModule {

    WorkspaceModuleId getId();

    File getModuleDir();

    File getBuildDir();

    Collection<String> getSourceClassifiers();

    boolean hasSources(String classifier);

    ArtifactSources getSources(String classifier);

    default boolean hasMainSources() {
        return hasSources(DefaultWorkspaceModule.MAIN);
    }

    default boolean hasTestSources() {
        return hasSources(DefaultWorkspaceModule.TEST);
    }

    default ArtifactSources getMainSources() {
        return getSources(DefaultWorkspaceModule.MAIN);
    }

    default ArtifactSources getTestSources() {
        return getSources(DefaultWorkspaceModule.TEST);
    }

    PathCollection getBuildFiles();

    default PathTree getContentTree(String classifier) {
        final ArtifactSources artifactSources = getSources(classifier);
        return artifactSources == null || !artifactSources.isOutputAvailable() ? EmptyPathTree.getInstance()
                : artifactSources.getOutputTree();
    }
}
