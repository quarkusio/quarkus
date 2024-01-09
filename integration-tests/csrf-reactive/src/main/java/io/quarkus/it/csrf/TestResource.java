package io.quarkus.it.csrf;

import java.io.File;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.reactive.RestForm;

import io.quarkus.csrf.reactive.runtime.CsrfTokenParameterProvider;
import io.quarkus.csrf.reactive.runtime.CsrfTokenUtils;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/service")
public class TestResource {

    @Inject
    Template csrfTokenForm;

    @Inject
    Template csrfTokenFirstForm;

    @Inject
    Template csrfTokenSecondForm;

    @Inject
    Template csrfTokenHeader;

    @Inject
    Template csrfTokenWithFormRead;

    @Inject
    Template csrfTokenMultipart;

    @Inject
    RoutingContext routingContext;

    @Inject
    CsrfTokenParameterProvider parameterProvider;

    @GET
    @Path("/csrfTokenForm")
    @Produces(MediaType.TEXT_HTML)
    @Authenticated
    public TemplateInstance getCsrfTokenForm() {
        return csrfTokenForm.instance();
    }

    @GET
    @Path("/csrfTokenFirstForm")
    @Produces(MediaType.TEXT_HTML)
    @Authenticated
    public TemplateInstance getCsrfTokenFirstForm() {
        return csrfTokenFirstForm.instance();
    }

    @GET
    @Path("/csrfTokenWithFormRead")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getCsrfTokenWithFormRead() {
        return csrfTokenWithFormRead.instance();
    }

    @GET
    @Path("/csrfTokenWithHeader")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getCsrfTokenWithHeader() {
        return csrfTokenHeader.instance();
    }

    @POST
    @Path("/csrfTokenForm")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenForm(@FormParam("name") String name, @HeaderParam("X-CSRF-TOKEN") String csrfHeader) {
        return name + ":" + routingContext.get("csrf_token_verified", false) + ":tokenHeaderIsSet=" + (csrfHeader != null);
    }

    @POST
    @Path("/csrfTokenFirstForm")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance postCsrfTokenFirstForm() {
        return csrfTokenSecondForm.instance();
    }

    @POST
    @Path("/csrfTokenSecondForm")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenSecondForm(@FormParam("name") String name, @HeaderParam("X-CSRF-TOKEN") String csrfHeader) {
        return name + ":" + routingContext.get("csrf_token_verified", false) + ":tokenHeaderIsSet=" + (csrfHeader != null);
    }

    @POST
    @Path("/csrfTokenWithFormRead")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenWithFormRead(@HeaderParam("X-CSRF-TOKEN") String csrfHeader) {
        return "verified:" + routingContext.get("csrf_token_verified", false) + ":tokenHeaderIsSet=" + (csrfHeader != null);
    }

    @POST
    @Path("/csrfTokenWithHeader")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON })
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenWithHeader(@HeaderParam("X-CSRF-TOKEN") String csrfHeader) {
        return "verified:" + routingContext.get("csrf_token_verified", false) + ":tokenHeaderIsSet=" + (csrfHeader != null);
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
            @CookieParam("csrftoken") Cookie csrfTokenCookie,
            @HeaderParam("X-CSRF-TOKEN") String csrfHeader) throws Exception {
        return name + ":" + routingContext.get("csrf_token_verified", false) + ":"
                + isResteasyReactiveUpload(multiPart.file) + ":"
                + csrfTokenCookie.getValue().equals(
                        CsrfTokenUtils.signCsrfToken(csrfTokenParam,
                                ConfigProvider.getConfig().getValue("quarkus.csrf-reactive.token-signature-key",
                                        String.class)))
                + ":tokenHeaderIsSet=" + (csrfHeader != null);
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

    @GET
    @Path("/token")
    @Produces(MediaType.TEXT_PLAIN)
    public String getToken() {
        return this.parameterProvider.getToken();
    }

    public static class MultiPart {
        @RestForm
        File file;
    }
}
