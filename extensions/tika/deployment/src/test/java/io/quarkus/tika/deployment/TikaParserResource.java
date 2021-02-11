package io.quarkus.tika.deployment;

import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.tika.TikaParser;

@Path("/parse")
public class TikaParserResource {
    @Inject
    TikaParser parser;

    @POST
    @Path("/text")
    @Consumes("application/pdf")
    @Produces(MediaType.TEXT_PLAIN)
    public String extractText(InputStream stream) {
        return parser.parse(stream).getText();
    }

}
