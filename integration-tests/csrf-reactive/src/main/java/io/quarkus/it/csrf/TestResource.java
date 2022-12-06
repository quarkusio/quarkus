package io.quarkus.it.csrf;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;

@Path("/service")
public class TestResource {

    @Inject
    Template csrfTokenForm;

    @Inject
    Template csrfTokenMultipart;

    @Inject
    RoutingContext routingContext;

    @GET
    @Path("/csrfTokenForm")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getCsrfTokenForm() {
        return csrfTokenForm.instance();
    }

    @POST
    @Path("/csrfTokenForm")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenForm(@FormParam("name") String name) {
        return name + ":" + routingContext.get("csrf_token_verified", false);
    }

    @GET
    @Path("/csrfTokenMultipart")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getCsrfTokenMultipart() {
        return csrfTokenMultipart.instance();
    }

    @POST
    @Path("/csrfTokenMultipart")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenMultipart(@FormParam("name") String name,
            @FormParam("csrf-token") String csrfTokenParam, @CookieParam("csrftoken") Cookie csrfTokenCookie) {
        return name + ":" + routingContext.get("csrf_token_verified", false) + ":"
                + csrfTokenCookie.getValue().equals(csrfTokenParam);
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimpleGet() {
        return "hello";
    }
}
