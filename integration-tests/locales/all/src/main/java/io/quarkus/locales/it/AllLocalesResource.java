package io.quarkus.locales.it;

import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

@Path("")
public class AllLocalesResource extends LocalesResource {
    private static final Logger LOG = Logger.getLogger(AllLocalesResource.class);

    // @Pattern validation does nothing when placed in LocalesResource.
    @GET
    @Path("/hibernate-validator-test-validation-message-locale/{id}/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response validationMessageLocale(
            @Pattern(regexp = "A.*", message = "{pattern.message}") @PathParam("id") String id) {
        LOG.infof("Triggering test: id: %s", id);
        return Response.ok(id).build();
    }
}
