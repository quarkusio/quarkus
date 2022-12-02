package io.quarkus.resteasy.reactive.server.test.multipart;

import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

public class MultipartOutputResponse extends FormDataBase {

    @RestForm
    // don't set a part type, use the default
    private String name;

    @FormParam("custom-surname")
    @PartType("text/plain")
    private String surname;

    @RestForm("custom-status")
    @PartType(MediaType.TEXT_PLAIN)
    private Status status;

    @RestForm
    private List<String> values;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
