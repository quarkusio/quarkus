package io.quarkus.rest.qute;

import io.quarkus.arc.Arc;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.SimplifiedResourceInfo;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

// TODO: We probably want to share this with quarkus-resteasy-qute somehow...
public final class RestTemplate {

    private RestTemplate() {
    }

    private static String getActionName() {
        QuarkusRestRequestContext otherHttpContextObject = (QuarkusRestRequestContext) Arc.container()
                .select(CurrentVertxRequest.class).get().getOtherHttpContextObject();
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
