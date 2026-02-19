package io.quarkus.it.nat.metadata;

import java.util.Locale;
import java.util.ResourceBundle;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Test resource for native-image metadata parsing.
 */
@Path("/native-image-metadata-parser")
public class NativeImageMetadataParserResource {

    @GET
    @Path("/access-resource-bundle/{locale}")
    @Produces(MediaType.TEXT_PLAIN)
    public String accessResourceBundle(@PathParam("locale") String localeString) {
        Locale locale = new Locale(localeString);
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
        return bundle.getString("hello");
    }
}
