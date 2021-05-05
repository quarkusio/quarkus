package io.quarkus.rest.client.reactive;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "hello2")
public interface HelloClient2 {
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/")
    String echo(String name);
}
