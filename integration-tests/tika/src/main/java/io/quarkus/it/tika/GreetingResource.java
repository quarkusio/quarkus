package io.quarkus.it.tika;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.tika.Content;
import io.quarkus.tika.Metadata;

@Path("/parse")
public class GreetingResource {

    @POST
    @Path("/text")
    @Consumes({ "text/plain", "application/pdf", "application/vnd.oasis.opendocument.text" })
    @Produces(MediaType.TEXT_PLAIN)
    public String extractText(Content content) {
        return content.getText();
    }

    @POST
    @Path("/metadata")
    @Consumes({ "text/plain", "application/pdf", "application/vnd.oasis.opendocument.text" })
    @Produces(MediaType.TEXT_PLAIN)
    public String extractMetadata(Metadata metadata) {
        return metadata.getNames().toString();
    }
}
