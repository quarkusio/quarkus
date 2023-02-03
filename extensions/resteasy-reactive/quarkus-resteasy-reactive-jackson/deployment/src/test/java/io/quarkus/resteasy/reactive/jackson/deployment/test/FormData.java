package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FormData {

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    public Map<String, Object> map;

    @FormParam("names")
    @PartType(MediaType.TEXT_PLAIN)
    public List<String> names;

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    public int[] numbers;

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    public List<Integer> numbers2;

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    @Valid
    public Person person;

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    public Person[] persons;

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    public List<Person> persons2;

    @RestForm("htmlFile")
    private FileUpload htmlPart;

    public FileUpload getHtmlPart() {
        return htmlPart;
    }

    public void setHtmlPart(FileUpload htmlPart) {
        this.htmlPart = htmlPart;
    }
}
