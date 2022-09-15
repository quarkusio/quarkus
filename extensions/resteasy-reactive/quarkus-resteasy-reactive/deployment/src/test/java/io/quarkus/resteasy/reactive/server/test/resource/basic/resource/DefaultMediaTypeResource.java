package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.util.Date;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

@Path("/")
public class DefaultMediaTypeResource {

    protected static final Logger logger = Logger.getLogger(DefaultMediaTypeResource.class.getName());

    @Path("postDateProduce")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response postDateProduce(String source) throws Exception {
        return Response.ok().entity(new Date(10000)).build();
    }

    @Path("postDate")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response postDate(String source) throws Exception {
        return Response.ok().entity(new Date(10000)).build();
    }

    @Path("postFooProduce")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response postFooProduce(String source) throws Exception {
        return Response.ok().entity(new DefaultMediaTypeCustomObject(8, 9)).build();
    }

    @Path("postFoo")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response postFoo(String source) throws Exception {
        return Response.ok().entity(new DefaultMediaTypeCustomObject(8, 9)).build();
    }

    @Path("postIntProduce")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response postIntProduce(String source) throws Exception {
        return Response.ok().entity(8).build();
    }

    @Path("postInt")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response postInt(String source) throws Exception {
        return Response.ok().entity(8).build();
    }

    @Path("postIntegerProduce")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response postIntegerProduce(String source) throws Exception {
        return Response.ok().entity(5).build();
    }

    @Path("postInteger")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response postInteger(String source) throws Exception {
        return Response.ok().entity(5).build();
    }
}
