package io.quarkus.deployment;

import java.nio.file.Path;

import org.jboss.jandex.IndexView;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

public final class ApplicationArchiveImpl extends MultiBuildItem implements ApplicationArchive {

    private final IndexView indexView;
    private final PathCollection rootDirs;
    private final PathCollection paths;
    private final ArtifactKey artifactKey;

    public ApplicationArchiveImpl(IndexView indexView, Path archiveRoot,
            Path archiveLocation, ArtifactKey artifactKey) {
        this(indexView, PathList.of(archiveRoot), PathList.of(archiveLocation), artifactKey);
    }

    public ApplicationArchiveImpl(IndexView indexView, PathCollection rootDirs, PathCollection paths,
            ArtifactKey artifactKey) {
        this.indexView = indexView;
        this.rootDirs = rootDirs;
        this.paths = paths;
        this.artifactKey = artifactKey;
    }

    @Override
    public IndexView getIndex() {
        return indexView;
    }

    @Override
    @Deprecated
    public Path getArchiveLocation() {
        return paths.iterator().next();
    }

    @Override
    @Deprecated
    public PathsCollection getRootDirs() {
        return PathsCollection.from(rootDirs);
    }

    @Override
    public PathCollection getRootDirectories() {
        return rootDirs;
    }

    @Override
    @Deprecated
    public PathsCollection getPaths() {
        return PathsCollection.from(paths);
    }

    @Override
    public PathCollection getResolvedPaths() {
        return paths;
    }

    @Override
    public AppArtifactKey getArtifactKey() {
        return artifactKey == null ? null
                : new AppArtifactKey(artifactKey.getGroupId(), artifactKey.getArtifactId(), artifactKey.getClassifier(),
                        artifactKey.getType());
    }

    @Override
    public ArtifactKey getKey() {
        return artifactKey;
    }
}
