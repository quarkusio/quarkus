package io.quarkus.it.csrf;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.csrf.reactive.runtime.CsrfTokenUtils;
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
                + csrfTokenCookie.getValue().equals(
                        CsrfTokenUtils.signCsrfToken(csrfTokenParam,
                                ConfigProvider.getConfig().getValue("quarkus.csrf-reactive.token-signature-key",
                                        String.class)));
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimpleGet() {
        return "hello";
    }
}
