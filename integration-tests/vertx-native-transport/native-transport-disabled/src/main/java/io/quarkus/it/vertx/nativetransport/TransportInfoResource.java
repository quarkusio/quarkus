package io.quarkus.it.vertx.nativetransport;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@Path("/transport")
public class TransportInfoResource {

    @Inject
    Vertx vertx;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getTransportInfo() {
        return new JsonObject()
                .put("type", vertx.isNativeTransportEnabled() ? "native" : "nio")
                .put("nativeTransportEnabled", vertx.isNativeTransportEnabled())
                .encode();
    }
}
