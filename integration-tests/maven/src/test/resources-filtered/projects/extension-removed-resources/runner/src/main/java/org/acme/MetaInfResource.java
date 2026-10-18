package org.acme;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/meta-inf")
public class MetaInfResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}")
    public String hello(@PathParam("name") String name) {
    	final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		var url = cl.getResource("META-INF/" + name);
		if(url == null) {
			throw new BadRequestException(Response.status(Status.BAD_REQUEST).build());
		}
		try(InputStream is = url.openStream()) {
			return new String(is.readAllBytes());
		} catch (IOException e) {
			throw new RuntimeException("Failed to read " + url, e);
		}
    }
}
