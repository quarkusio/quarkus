package io.quarkus.resteasy.reactive.qute;

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.SimplifiedResourceInfo;

import io.quarkus.arc.Arc;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

// TODO: We probably want to share this with quarkus-resteasy-qute somehow...
public final class RestTemplate {

    private RestTemplate() {
    }

    private static String getActionName() {
        ResteasyReactiveRequestContext otherHttpContextObject = CurrentRequestManager.get();
        SimplifiedResourceInfo resourceMethod = otherHttpContextObject.getTarget().getSimplifiedResourceInfo();
        return resourceMethod.getResourceClass().getSimpleName() + "/" + resourceMethod.getMethodName();
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
