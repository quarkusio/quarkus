package io.quarkus.deployment;

import java.io.Closeable;
import java.nio.file.Path;

import org.jboss.jandex.IndexView;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.item.MultiBuildItem;

public final class ApplicationArchiveImpl extends MultiBuildItem implements ApplicationArchive {

    private final IndexView indexView;
    private final PathsCollection rootDirs;
    private final boolean jar;
    private final PathsCollection paths;

    public ApplicationArchiveImpl(IndexView indexView, Path archiveRoot, Closeable closeable, boolean jar,
            Path archiveLocation) {
        this.indexView = indexView;
        this.rootDirs = PathsCollection.of(archiveRoot);
        this.jar = jar;
        this.paths = PathsCollection.of(archiveLocation);
    }

    public ApplicationArchiveImpl(IndexView indexView, PathsCollection rootDirs, PathsCollection paths) {
        this.indexView = indexView;
        this.rootDirs = rootDirs;
        this.paths = paths;
        jar = false;
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
}
