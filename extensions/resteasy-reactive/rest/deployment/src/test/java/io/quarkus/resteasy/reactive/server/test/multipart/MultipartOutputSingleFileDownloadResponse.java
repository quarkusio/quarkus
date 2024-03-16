package io.quarkus.resteasy.reactive.server.test.multipart;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileDownload;

public class MultipartOutputSingleFileDownloadResponse {

    @RestForm
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    FileDownload file;
}
