package io.quarkus.resteasy.reactive.qute.deployment;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@Path("type-error")
public class TypeErrorResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance typeError3(String name);
    }

    @Path("native/type-error3")
    @GET
    public TemplateInstance nativeTypeError3() {
        return Templates.typeError3("world");
    }
}
