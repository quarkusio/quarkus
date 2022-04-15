package io.quarkus.it.rest.client.main;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
