package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

@MultipartForm
public class Payload {
    @RestForm("name")
    @PartType(MediaType.TEXT_PLAIN)
    public String name;
}
