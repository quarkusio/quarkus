package io.quarkus.qute.resteasy.deployment;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.resteasy.qute.RestTemplate;

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

        static native TemplateInstance typedTemplatePrimitives(boolean bool, byte b, short s, int i, long l, char c,
                float f, double d);
    }

    @Inject
    Template hello;

    @GET
    public TemplateInstance get(@QueryParam String name) {
        if (name == null) {
            name = "world";
        }
        return hello.data("name", name);
    }

    @Path("no-injection")
    @GET
    public TemplateInstance hello(@QueryParam String name) {
        if (name == null) {
            name = "world";
        }
        return RestTemplate.data("name", name);
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
    public TemplateInstance nativeTypedTemplate(@QueryParam String name) {
        if (name == null) {
            name = "world";
        }
        return Templates.typedTemplate(name, null);
    }

    @Path("native/toplevel")
    @GET
    public TemplateInstance nativeToplevelTypedTemplate(@QueryParam String name) {
        if (name == null) {
            name = "world";
        }
        return io.quarkus.qute.resteasy.deployment.Templates.toplevel(name);
    }
}
