package io.quarkus.qute.resteasy.deployment;

import java.util.Map;

import jakarta.ws.rs.Path;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@Path("missing-template")
public class MissingTemplateResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance hello(String name, Map<String, Object> other);

        public static native TemplateInstance missingTemplate();
    }
}
