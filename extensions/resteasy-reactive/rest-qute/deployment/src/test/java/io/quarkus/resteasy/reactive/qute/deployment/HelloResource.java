package io.quarkus.resteasy.reactive.qute.deployment;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.ResponseHeader;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.resteasy.reactive.qute.RestTemplate;
import io.smallrye.mutiny.Uni;

@Path("hello")
public class HelloResource {

    @CheckedTemplate
    public static class Templates {
        // GENERATED
        //        {
        //            Template template = Arc.container().instance(Engine.class).get().getTemplate("HelloResource/typedTemplate");
        //            TemplateInstance instance = template.instance();
        //            instance.data("name", name);
        //            return instance;
        //        }

        public static native TemplateInstance typedTemplate(String name, Map<String, Object> other);

        public static native TemplateInstance typedTemplatePrimitives(boolean bool, byte b, short s, int i, long l, char c,
                float f, double d);
    }

    @Inject
    Template hello;

    @GET
    public TemplateInstance get(@QueryParam("name") String name) {
        if (name == null) {
            name = "world";
        }
        return hello.data("name", name);
    }

    @Path("no-injection")
    @GET
    public Uni<TemplateInstance> hello(@QueryParam("name") String name) {
        if (name == null) {
            name = "world";
        }
        return Uni.createFrom().item(RestTemplate.data("name", name));
    }

    @Path("type-error")
    @GET
    public TemplateInstance typeError() {
        return RestTemplate.data("name", "world");
    }

    @Path("native/typed-template-primitives")
    @GET
    public TemplateInstance typedTemplatePrimitives() {
        return Templates.typedTemplatePrimitives(true, (byte) 0, (short) 1, 2, 3, 'a', 4.0f, 5.0);
    }

    @Path("native/typed-template")
    @GET
    public TemplateInstance nativeTypedTemplate(@QueryParam("name") String name) {
        if (name == null) {
            name = "world";
        }
        return Templates.typedTemplate(name, null);
    }

    @Path("native/toplevel")
    @GET
    public TemplateInstance nativeToplevelTypedTemplate(@QueryParam("name") String name) {
        if (name == null) {
            name = "world";
        }
        return io.quarkus.resteasy.reactive.qute.deployment.Templates.toplevel(name);
    }

    @ResponseStatus(201)
    @ResponseHeader(name = "foo", value = { "bar" })
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("status-and-headers")
    public TemplateInstance setStatusAndHeaders() {
        return hello.data("name", "world");
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("rest-response")
    public RestResponse<TemplateInstance> restResponse() {
        return RestResponse.status(RestResponse.Status.ACCEPTED, hello.data("name", "world"));
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("response")
    public Response response() {
        return Response.status(203).entity(hello.data("name", "world")).build();
    }
}
