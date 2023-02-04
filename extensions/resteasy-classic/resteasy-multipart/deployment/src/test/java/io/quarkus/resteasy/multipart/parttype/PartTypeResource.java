package io.quarkus.resteasy.multipart.parttype;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

@Path("/test/part-type/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class PartTypeResource {

    @POST
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    @Transactional
    public Response reproduce(@MultipartForm PartTypeDto reproduceDto) {
        return Response.ok().build();
    }
}
