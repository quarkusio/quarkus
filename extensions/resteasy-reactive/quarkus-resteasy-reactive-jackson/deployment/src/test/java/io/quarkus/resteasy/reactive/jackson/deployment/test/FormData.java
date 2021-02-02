package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FormData {

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    public Map<String, Object> map;

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    public Person person;

    @RestForm("htmlFile")
    private FileUpload htmlPart;

    public FileUpload getHtmlPart() {
        return htmlPart;
    }

    public void setHtmlPart(FileUpload htmlPart) {
        this.htmlPart = htmlPart;
    }
}
