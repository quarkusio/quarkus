package com.example.model;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/model")
@ApplicationScoped
public class ModelResource {
    @GET
    public String hello() {
        return "Hello model";
    }
}
