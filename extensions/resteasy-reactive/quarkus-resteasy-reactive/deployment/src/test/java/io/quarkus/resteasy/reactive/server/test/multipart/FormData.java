package io.quarkus.resteasy.reactive.server.test.multipart;

import java.io.File;
import java.nio.file.Path;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FormData extends FormDataBase {

    @RestForm
    // don't set a part type, use the default
    private String name;

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    private Status status;

    @RestForm("htmlFile")
    private FileUpload htmlPart;

    @RestForm("xmlFile")
    public Path xmlPart;

    @RestForm
    public File txtFile;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public FileUpload getHtmlPart() {
        return htmlPart;
    }

    public void setHtmlPart(FileUpload htmlPart) {
        this.htmlPart = htmlPart;
    }
}
