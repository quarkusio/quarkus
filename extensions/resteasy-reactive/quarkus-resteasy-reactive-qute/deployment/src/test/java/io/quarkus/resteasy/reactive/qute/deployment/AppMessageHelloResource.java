package io.quarkus.resteasy.reactive.qute.deployment;

import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.MessageBundles;

@Path("hello")
public class AppMessageHelloResource {

    @Inject
    Template hello;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get() {
        return hello.instance();
    }

    @GET
    @Path("de")
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance helloDe() {
        return hello.instance().setAttribute(MessageBundles.ATTRIBUTE_LOCALE, Locale.GERMAN);
    }
}
