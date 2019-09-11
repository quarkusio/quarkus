package io.quarkus.it.yaml.configuration;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Path(CorsYamlConfigTestResource.PATH)
public class CorsYamlConfigTestResource {

    public static final String PATH = "/config";

    @GET
    public void get() {
    }

    @POST
    public void post() {
    }

    @PUT
    public void put() {
    }
}
