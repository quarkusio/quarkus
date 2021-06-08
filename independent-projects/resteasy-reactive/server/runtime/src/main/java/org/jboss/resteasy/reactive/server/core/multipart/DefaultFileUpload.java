package org.jboss.resteasy.reactive.server.core.multipart;

import java.io.IOException;
import java.nio.file.Path;
import javax.ws.rs.core.HttpHeaders;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class DefaultFileUpload implements FileUpload {

    private final String name;
    private final FormData.FormValue fileUpload;

    public DefaultFileUpload(String name, FormData.FormValue fileUpload) {
        this.name = name;
        this.fileUpload = fileUpload;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Path uploadedFile() {
        return fileUpload.getFileItem().getFile();
    }

    @Override
    public String fileName() {
        return fileUpload.getFileName();
    }

    @Override
    public long size() {
        try {
            return fileUpload.getFileItem().getFileSize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String contentType() {
        return fileUpload.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
    }

    @Override
    public String charSet() {
        String ct = fileUpload.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (ct == null) {
            return null;
        }
        return HeaderUtil.extractQuotedValueFromHeader(ct, "charset");
    }
}
