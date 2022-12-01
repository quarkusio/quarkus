package io.quarkus.resteasy.reactive.server.test.multipart;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FormDataSameFileName {

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    public Status status;

    @RestForm("htmlFile")
    private List<File> htmlFiles;

    @RestForm("txtFile")
    public List<FileUpload> txtFiles;

    @RestForm("xmlFile")
    public List<Path> xmlFiles;

    public List<File> getHtmlFiles() {
        return htmlFiles;
    }

    public void setHtmlFiles(List<File> htmlFiles) {
        this.htmlFiles = htmlFiles;
    }
}
