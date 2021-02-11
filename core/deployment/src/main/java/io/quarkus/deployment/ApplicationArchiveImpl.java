package io.quarkus.deployment;

import java.io.Closeable;
import java.nio.file.Path;

import org.jboss.jandex.IndexView;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.item.MultiBuildItem;

public final class ApplicationArchiveImpl extends MultiBuildItem implements ApplicationArchive {

    private final IndexView indexView;
    private final PathsCollection rootDirs;
    private final boolean jar;
    private final PathsCollection paths;
    private final AppArtifactKey artifactKey;

    public ApplicationArchiveImpl(IndexView indexView, Path archiveRoot, Closeable closeable, boolean jar,
            Path archiveLocation, AppArtifactKey artifactKey) {
        this(indexView, PathsCollection.of(archiveRoot), PathsCollection.of(archiveLocation), artifactKey);
    }

    public ApplicationArchiveImpl(IndexView indexView, PathsCollection rootDirs, PathsCollection paths,
            AppArtifactKey artifactKey) {
        this(indexView, rootDirs, paths, false, artifactKey);
    }

    private ApplicationArchiveImpl(IndexView indexView, PathsCollection rootDirs, PathsCollection paths, boolean jar,
            AppArtifactKey artifactKey) {
        this.indexView = indexView;
        this.rootDirs = rootDirs;
        this.paths = paths;
        this.jar = jar;
        this.artifactKey = artifactKey;
    }

    @Override
    public IndexView getIndex() {
        return indexView;
    }

    @Override
    @Deprecated
    public Path getArchiveRoot() {
        return rootDirs.iterator().next();
    }

    @Override
    @Deprecated
    public boolean isJarArchive() {
        return jar;
    }

    @Override
    @Deprecated
    public Path getArchiveLocation() {
        return paths.iterator().next();
    }

    @Override
    public PathsCollection getRootDirs() {
        return rootDirs;
    }

    @Override
    public PathsCollection getPaths() {
        return paths;
    }

    @Override
    public AppArtifactKey getArtifactKey() {
        return artifactKey;
    }

}
