package io.quarkus.it.spring.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;

@Path("/greeting")
public class GreetingResource {

    @Autowired
    GreetingService greetingService;

    @GET
    public String greeting(@QueryParam("name") String name) {
        return greetingService.greet(name);
    }
}
