package io.quarkus.it.rest.client;

import java.io.InputStream;
import java.util.UUID;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class MultipartBody {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream file;

    @FormParam("fileName")
    @PartType(MediaType.TEXT_PLAIN)
    public String fileName;

    @FormParam("uuid")
    @PartType(MediaType.TEXT_PLAIN)
    public UUID uuid;
}
