package io.quarkus.compressors.it;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Resource with endpoints that consume compressed data
 * in POST and PUT bodies from the client.
 * Depending on the accept-encoding, the data is then
 * compressed again and sent to the client
 * </br>
 * e.g. Client sends a gzipped POST body and receives
 * a brotli compressed response body.
 * </br>
 * The endpoint looks like a dummy echo service, but
 * there is compression and decompression going on behind
 * the scenes in Vert.x. -> Netty.
 * </br>
 * See: https://github.com/quarkusio/quarkus/pull/44348
 */
public class DecompressResource {

    @POST
    @Path("/text")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String textPost(String text) {
        return text;
    }

    @PUT
    @Path("/text")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String textPut(String text) {
        return text;
    }

    @POST
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String jsonPost(String json) {
        return json;
    }

    @PUT
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String jsonPut(String json) {
        return json;
    }

    @POST
    @Path("/xml")
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.TEXT_XML)
    public String xmlPost(String xml) {
        return xml;
    }

    @PUT
    @Path("/xml")
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.TEXT_XML)
    public String xmlPut(String xml) {
        return xml;
    }

    @POST
    @Path("/xhtml")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    @Consumes(MediaType.APPLICATION_XHTML_XML)
    public String xhtmlPost(String xhtml) {
        return xhtml;
    }

    @PUT
    @Path("/xhtml")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    @Consumes(MediaType.APPLICATION_XHTML_XML)
    public String xhtmlPut(String xhtml) {
        return xhtml;
    }
}
