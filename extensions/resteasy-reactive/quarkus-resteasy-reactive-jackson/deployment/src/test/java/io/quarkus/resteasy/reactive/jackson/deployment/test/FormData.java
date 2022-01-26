package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FormData {

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    public Map<String, Object> map;

    @FormParam("names")
    @PartType(MediaType.TEXT_PLAIN)
    public String[] names;

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    public int[] numbers;

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    public Person person;

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    public Person[] persons;

    @RestForm("htmlFile")
    private FileUpload htmlPart;

    public FileUpload getHtmlPart() {
        return htmlPart;
    }

    public void setHtmlPart(FileUpload htmlPart) {
        this.htmlPart = htmlPart;
    }
}
