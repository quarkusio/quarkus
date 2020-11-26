package io.quarkus.qute.rest.deployment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;

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
