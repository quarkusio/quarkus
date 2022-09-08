package io.quarkus.it.csrf;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;

@Path("/service")
public class TestResource {

    @Inject
    Template csrfToken;

    @Inject
    RoutingContext routingContext;

    @GET
    @Path("/csrfTokenForm")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getCsrfTokenForm() {
        return csrfToken.instance();
    }

    @POST
    @Path("/csrfTokenForm")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenForm(@FormParam("name") String name) {
        return name + ":" + routingContext.get("csrf_token_verified", false);
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimpleGet() {
        return "hello";
    }
}
