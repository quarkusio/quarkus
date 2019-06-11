package io.quarkus.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.MultiBuildItem;

public final class ApplicationArchiveImpl extends MultiBuildItem implements ApplicationArchive, Closeable {

    private final IndexView indexView;
    private final Path archiveRoot;
    private final Closeable closeable;
    private final boolean jar;
    private final Path archiveLocation;

    public ApplicationArchiveImpl(IndexView indexView, Path archiveRoot, Closeable closeable, boolean jar,
            Path archiveLocation) {
        this.indexView = indexView;
        this.archiveRoot = archiveRoot;
        this.closeable = closeable;
        this.jar = jar;
        this.archiveLocation = archiveLocation;
    }

    @Override
    public IndexView getIndex() {
        return indexView;
    }

    @Override
    public Path getArchiveRoot() {
        return archiveRoot;
    }

    @Override
    public boolean isJarArchive() {
        return jar;
    }

    @Override
    public Path getArchiveLocation() {
        return archiveLocation;
    }

    @Override
    public void close() throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }
}
