package org.acme.gradledemo;

import org.acme.gradledemo.api.Dog;
import org.acme.gradledemo.dogs.DemoDogDirectory;
import org.acme.gradledemo.extension.DemoGreeting;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/dogs")
public class DogsResource {

    @Inject
    DemoGreeting greeting;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String dogs() {
        String names = new DemoDogDirectory().dogs().stream().map(Dog::name).reduce((left, right) -> left + ", " + right)
                .orElse("no dogs");
        return DemoMessage.APPLICATION_MESSAGE + "; " + greeting.message() + "; " + DemoBuildMessage.message() + "; dogs: "
                + names;
    }
}
