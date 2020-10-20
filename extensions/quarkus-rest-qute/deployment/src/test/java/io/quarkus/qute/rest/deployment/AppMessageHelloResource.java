package io.quarkus.qute.rest.deployment;

import java.util.Locale;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
