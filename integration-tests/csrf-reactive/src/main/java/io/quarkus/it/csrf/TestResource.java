package io.quarkus.it.csrf;

import java.io.File;

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
import org.jboss.resteasy.reactive.RestForm;

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
            @FormParam("csrf-token") String csrfTokenParam,
            MultiPart multiPart,
            @CookieParam("csrftoken") Cookie csrfTokenCookie) throws Exception {
        return name + ":" + routingContext.get("csrf_token_verified", false) + ":"
                + isResteasyReactiveUpload(multiPart.file) + ":"
                + csrfTokenCookie.getValue().equals(
                        CsrfTokenUtils.signCsrfToken(csrfTokenParam,
                                ConfigProvider.getConfig().getValue("quarkus.csrf-reactive.token-signature-key",
                                        String.class)));
    }

    private boolean isResteasyReactiveUpload(File file) throws Exception {
        return file.getName().startsWith("resteasy-reactive");
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimpleGet() {
        return "hello";
    }

    public static class MultiPart {
        @RestForm
        File file;
    }
}
