package org.jboss.resteasy.reactive.client.impl.multipart;

import java.io.IOException;
import java.nio.file.Path;

import org.jboss.resteasy.reactive.multipart.FileDownload;

import io.netty.handler.codec.http.multipart.FileUpload;

public class FileDownloadImpl implements FileDownload {

    // we're using netty's file upload to represent download too
    private final FileUpload file;

    public FileDownloadImpl(FileUpload httpData) {
        this.file = httpData;
    }

    @Override
    public String name() {
        return file.getName();
    }

    @Override
    public Path filePath() {
        try {
            return file == null ? null : file.getFile().toPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to provide file for download", e);
        }
    }

    @Override
    public String fileName() {
        return file.getFilename();
    }

    @Override
    public long size() {
        throw new UnsupportedOperationException("returning size of a downloaded file is not supported");
    }

    @Override
    public String contentType() {
        return file.getContentType();
    }

    @Override
    public String charSet() {
        return file.getCharset().name();
    }
}
