package io.quarkus.qrs.test.resource.basic.resource;

import org.junit.Assert;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Locale;

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
      Assert.assertEquals("Wrong number of accepted languages", 1, languages.size());
      Assert.assertEquals("Wrong accepted language", new Locale("en", "us"), languages.get(0));
      Assert.assertEquals("Wrong accepted language", MediaType.valueOf("text/html"), headers.getAcceptableMediaTypes().get(0));
      return "html";
   }

   @GET
   @Path("/stuff.old")
   @Produces("text/plain")
   public String getJson(@Context HttpHeaders headers) {
      List<Locale> languages = headers.getAcceptableLanguages();
      Assert.assertEquals("Wrong number of accepted languages", 1, languages.size());
      Assert.assertEquals("Wrong accepted language", new Locale("en", "us"), languages.get(0));
      Assert.assertEquals("Wrong accepted language", MediaType.valueOf("text/plain"), headers.getAcceptableMediaTypes().get(0));
      return "plain";
   }
}
