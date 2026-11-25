package io.quarkus.it.rest.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/client/primitive")
public class PrimitiveClientResource {

    @RestClient
    PrimitiveClient client;

    @GET
    @Path("/{type}")
    @Produces(MediaType.TEXT_PLAIN)
    public String call(@PathParam("type") String type, @QueryParam("value") String value) {
        return switch (type) {
            case "long" -> String.valueOf(client.getLong(value));
            case "int" -> String.valueOf(client.getInt(value));
            case "double" -> String.valueOf(client.getDouble(value));
            case "boolean" -> String.valueOf(client.getBoolean(value));
            case "Long" -> String.valueOf(client.getLongWrapper(value));
            default -> throw new IllegalArgumentException("Unsupported type " + type);
        };
    }
}
