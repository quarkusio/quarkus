package io.quarkus.aesh.websocket.deployment;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.aesh.runtime.AeshCommandMetadata;
import io.quarkus.aesh.runtime.AeshContext;

@Path("/dev-test")
public class DevModeCommandInfoEndpoint {

    @Inject
    AeshContext aeshContext;

    @GET
    @Path("/commands")
    @Produces(MediaType.TEXT_PLAIN)
    public String commands() {
        StringBuilder sb = new StringBuilder();
        for (AeshCommandMetadata cmd : aeshContext.getCommands()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(cmd.getCommandName());
        }
        return sb.toString();
    }
}
