package com.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/service")
@ApplicationScoped
public class ServiceResource {
    @GET
    public String hello() {
        return "Hello service";
    }
}
