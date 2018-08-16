package org.jboss.shamrock.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import org.jboss.jandex.IndexView;

public class ApplicationArchiveImpl implements ApplicationArchive, Closeable {

    private final IndexView indexView;
    private final Path archiveRoot;
    private final Closeable closeable;

    public ApplicationArchiveImpl(IndexView indexView, Path archiveRoot, Closeable closeable) {
        this.indexView = indexView;
        this.archiveRoot = archiveRoot;
        this.closeable = closeable;
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
    public void close() throws IOException {
        if(closeable != null) {
            closeable.close();
        }
    }
}
