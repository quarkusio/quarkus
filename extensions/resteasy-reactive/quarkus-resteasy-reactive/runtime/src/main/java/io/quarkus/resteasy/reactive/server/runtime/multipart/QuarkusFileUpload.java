package io.quarkus.resteasy.reactive.server.runtime.multipart;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.resteasy.reactive.multipart.FileUpload;

public class QuarkusFileUpload implements FileUpload {

    private final io.vertx.ext.web.FileUpload vertxFileUpload;

    public QuarkusFileUpload(io.vertx.ext.web.FileUpload vertxFileUpload) {
        this.vertxFileUpload = vertxFileUpload;
    }

    @Override
    public String name() {
        return vertxFileUpload.name();
    }

    @Override
    public Path uploadedFile() {
        return Paths.get(vertxFileUpload.uploadedFileName());
    }

    @Override
    public String fileName() {
        return vertxFileUpload.fileName();
    }

    @Override
    public long size() {
        return vertxFileUpload.size();
    }

    @Override
    public String contentType() {
        return vertxFileUpload.contentType();
    }

    @Override
    public String charSet() {
        return vertxFileUpload.charSet();
    }
}
