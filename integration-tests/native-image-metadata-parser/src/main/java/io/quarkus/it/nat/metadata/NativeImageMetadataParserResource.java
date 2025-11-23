package io.quarkus.it.nat.metadata;

import java.util.ResourceBundle;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * Test resource for native-image metadata parsing.
 */
@Path("/native-image-metadata-parser")
public class NativeImageMetadataParserResource {

    @GET
    @Path("/test")
    public String test() {
        return "Native image metadata parser test";
    }

    @GET
    @Path("/bundle/{locale}")
    public String getBundleMessage(@PathParam("locale") String locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", new java.util.Locale(locale));
        return bundle.getString("greeting");
    }
}
