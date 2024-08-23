package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient
@RegisterProvider(ParamConverter.class)
public interface ParamClient {

    @POST
    @Path("/param")
    @Produces(MediaType.TEXT_PLAIN)
    String getParam(@QueryParam("param") Param param);
}
