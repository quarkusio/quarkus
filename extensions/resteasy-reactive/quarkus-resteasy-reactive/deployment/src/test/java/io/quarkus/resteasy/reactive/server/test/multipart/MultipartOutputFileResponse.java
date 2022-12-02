package io.quarkus.resteasy.reactive.server.test.multipart;

import java.io.File;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

public class MultipartOutputFileResponse {

    @RestForm
    String name;

    @RestForm
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    File file;
}
