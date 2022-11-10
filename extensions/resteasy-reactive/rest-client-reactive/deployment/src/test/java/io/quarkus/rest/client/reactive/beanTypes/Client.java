package io.quarkus.rest.client.reactive.beanTypes;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "hello2")
public interface Client extends Alpha {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String test();

}
