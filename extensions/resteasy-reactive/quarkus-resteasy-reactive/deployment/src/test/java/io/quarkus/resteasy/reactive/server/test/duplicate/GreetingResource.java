package io.quarkus.resteasy.reactive.server.test.duplicate;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello-resteasy")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String helloGet() {
        return "Hello RESTEasy";
    }

    @GET
    @Consumes("*/*")
    @Produces(MediaType.TEXT_PLAIN)
    public String helloGetWildcard() {
        return "Hello RESTEasy";
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String helloGetXML() {
        return "Hello XML";
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String helloPost(String yo) {
        return "Hello RESTEasy";
    }

    @GET
    public String helloGetNoExplicitMimeType(String yo) {
        return "Hello RESTEasy";
    }

    @POST
    public Message helloPostNoExplicit(String yo) {
        return new Message("Hello RESTEasy");
    }

    private class Message {
        private String value;

        public Message(String value) {
            this.value = value;
        }
    }
}
