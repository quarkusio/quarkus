package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class DefaultCharsetResource {

    @GET
    @Produces("text/plain")
    @Path("nocharset")
    public String noCharset() {
        return "ok";
    }

    @GET
    @Produces("text/plain; charset=UTF-16")
    @Path("charset")
    public String charset() {
        return "ok";
    }

    @GET
    @Path("nomediatype")
    public String noMediaType() {
        return "ok";
    }

    @GET
    @Produces("application/xml")
    @Path("xml_nocharset")
    public String xmlNoCharset() {
        return "ok";
    }

    @GET
    @Produces("application/xml; charset=UTF-16")
    @Path("xml_charset")
    public String xmlCharset() {
        return "ok";
    }

    @GET
    @Produces("application/xml-external-parsed-entity")
    @Path("external")
    public String external() {
        return "ok";
    }

    @GET
    @Produces("application/json")
    @Path("json")
    public String json() {
        return "ok";
    }
}
