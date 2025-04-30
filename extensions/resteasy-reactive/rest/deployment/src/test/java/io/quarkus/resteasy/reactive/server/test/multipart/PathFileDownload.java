package io.quarkus.resteasy.reactive.server.test.multipart;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.resteasy.reactive.multipart.FileDownload;

public class PathFileDownload implements FileDownload {
    private final String partName;
    private final File file;

    public PathFileDownload(File file) {
        this(null, file);
    }

    public PathFileDownload(String partName, File file) {
        this.partName = partName;
        this.file = file;
    }

    @Override
    public String name() {
        return partName;
    }

    @Override
    public Path filePath() {
        return file.toPath();
    }

    @Override
    public String fileName() {
        return file.getName();
    }

    @Override
    public long size() {
        try {
            return Files.size(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to get size", e);
        }
    }

    @Override
    public String contentType() {
        return null;
    }

    @Override
    public String charSet() {
        return null;
    }
}
