package io.quarkus.resteasy.qute;

import jakarta.ws.rs.container.ResourceInfo;

import org.jboss.resteasy.core.ResteasyContext;

import io.quarkus.arc.Arc;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

public final class RestTemplate {

    private RestTemplate() {
    }

    private static String getActionName() {
        ResourceInfo resourceMethod = ResteasyContext.getContextData(ResourceInfo.class);
        return resourceMethod.getResourceClass().getSimpleName() + "/" + resourceMethod.getResourceMethod().getName();
    }

    public static TemplateInstance data(String name, Object value) {
        Template template = Arc.container().instance(Engine.class).get().getTemplate(getActionName());
        return template.data(name, value);
    }

    public static TemplateInstance data(Object data) {
        Template template = Arc.container().instance(Engine.class).get().getTemplate(getActionName());
        return template.data(data);
    }
}
