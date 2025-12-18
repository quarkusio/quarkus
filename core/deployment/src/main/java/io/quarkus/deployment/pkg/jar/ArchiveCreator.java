package io.quarkus.deployment.pkg.jar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Manifest;

public interface ArchiveCreator extends AutoCloseable {

    static final String UNKNOWN_SOURCE = "Unknown source";
    static final String CURRENT_APPLICATION = "Current application";

    void addManifest(Manifest manifest) throws IOException;

    void addDirectory(String directory, String source) throws IOException;

    void addFile(Path origin, String target, String source) throws IOException;

    void addFile(byte[] bytes, String target, String source) throws IOException;

    void addFileIfNotExists(Path origin, String target, String source) throws IOException;

    void addFileIfNotExists(byte[] bytes, String target, String source) throws IOException;

    void addFile(List<byte[]> bytes, String target, String source) throws IOException;

    default void addDirectory(String directory) throws IOException {
        addDirectory(directory, UNKNOWN_SOURCE);
    }

    default void addFile(Path origin, String target) throws IOException {
        addFile(origin, target, UNKNOWN_SOURCE);
    }

    default void addFile(byte[] bytes, String target) throws IOException {
        addFile(bytes, target, UNKNOWN_SOURCE);
    }

    default void addFileIfNotExists(Path origin, String target) throws IOException {
        addFileIfNotExists(origin, target, UNKNOWN_SOURCE);
    }

    default void addFileIfNotExists(byte[] bytes, String target) throws IOException {
        addFileIfNotExists(bytes, target, UNKNOWN_SOURCE);
    }

    default void addFile(List<byte[]> bytes, String target) throws IOException {
        addFile(bytes, target, UNKNOWN_SOURCE);
    }

    boolean isMultiVersion();

    void makeMultiVersion() throws IOException;

    @Override
    void close();
}
