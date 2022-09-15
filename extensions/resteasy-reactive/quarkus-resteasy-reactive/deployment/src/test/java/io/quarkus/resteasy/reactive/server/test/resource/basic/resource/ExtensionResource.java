package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.util.List;
import java.util.Locale;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Assertions;

@Path("/extension")
public class ExtensionResource {
    @GET
    @Produces("*/*")
    public String getDefault() {
        return "default";
    }

    @GET
    @Produces("application/xml")
    public String getXml(@Context HttpHeaders headers) {
        @SuppressWarnings("unused")
        List<Locale> languages = headers.getAcceptableLanguages();
        @SuppressWarnings("unused")
        List<MediaType> mediaTypes = headers.getAcceptableMediaTypes();
        return "xml";
    }

    @GET
    @Produces("text/html")
    public String getXmlTwo(@Context HttpHeaders headers) {
        List<Locale> languages = headers.getAcceptableLanguages();
        Assertions.assertEquals(1, languages.size(), "Wrong number of accepted languages");
        Assertions.assertEquals(new Locale("en", "us"), languages.get(0), "Wrong accepted language");
        Assertions.assertEquals(MediaType.valueOf("text/html"),
                headers.getAcceptableMediaTypes().get(0), "Wrong accepted language");
        return "html";
    }

    @GET
    @Path("/stuff.old")
    @Produces("text/plain")
    public String getJson(@Context HttpHeaders headers) {
        List<Locale> languages = headers.getAcceptableLanguages();
        Assertions.assertEquals(1, languages.size(), "Wrong number of accepted languages");
        Assertions.assertEquals(new Locale("en", "us"), languages.get(0), "Wrong accepted language");
        Assertions.assertEquals(MediaType.valueOf("text/plain"),
                headers.getAcceptableMediaTypes().get(0), "Wrong accepted language");
        return "plain";
    }
}
