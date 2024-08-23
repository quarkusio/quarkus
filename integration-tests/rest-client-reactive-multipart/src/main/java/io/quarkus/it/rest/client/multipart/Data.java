package io.quarkus.it.rest.client.multipart;

import jakarta.ws.rs.FormParam;

import org.jboss.resteasy.reactive.PartType;

public class Data {
    @FormParam("text")
    @PartType("text/plain")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
