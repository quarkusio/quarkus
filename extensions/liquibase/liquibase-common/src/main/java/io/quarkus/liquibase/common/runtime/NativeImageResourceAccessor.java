package io.quarkus.liquibase.common.runtime;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import liquibase.resource.AbstractPathResourceAccessor;
import liquibase.resource.PathResource;
import liquibase.resource.Resource;

public class NativeImageResourceAccessor extends AbstractPathResourceAccessor {
    private static final URI NATIVE_IMAGE_FILESYSTEM_URI = URI.create("resource:/");
    private static final Logger log = Logger.getLogger(NativeImageResourceAccessor.class);

    private final FileSystem fileSystem;

    public NativeImageResourceAccessor() {
        FileSystem fs;
        try {
            fs = FileSystems.newFileSystem(
                    NATIVE_IMAGE_FILESYSTEM_URI,
                    Collections.singletonMap("create", "true"));
            log.debug("Creating new filesystem for native image");
        } catch (FileSystemAlreadyExistsException ex) {
            fs = FileSystems.getFileSystem(NATIVE_IMAGE_FILESYSTEM_URI);
            log.debug("Native image file system already exists", ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        fileSystem = fs;
    }

    @Override
    protected Path getRootPath() {
        return fileSystem.getPath("/");
    }

    @Override
    protected Resource createResource(Path file, String pathToAdd) {
        return new PathResource(pathToAdd, file);
    }

    @Override
    public void close() {
    }

    @Override
    public List<String> describeLocations() {
        return Collections.singletonList(fileSystem.toString());
    }

    @Override
    public String toString() {
        return getClass().getName() + " (" + getRootPath() + ") (" + fileSystem.toString() + ")";
    }
}
