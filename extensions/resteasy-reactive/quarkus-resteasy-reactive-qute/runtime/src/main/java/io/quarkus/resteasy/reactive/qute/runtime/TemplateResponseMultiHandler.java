package io.quarkus.resteasy.reactive.qute.runtime;

import static io.quarkus.resteasy.reactive.qute.runtime.Util.setSelectedVariant;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Multi;

public class TemplateResponseMultiHandler implements ServerRestHandler {

    private volatile Engine engine;

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        Object result = requestContext.getResult();
        if (!(result instanceof TemplateInstance)) {
            return;
        }

        if (engine == null) {
            synchronized (this) {
                if (engine == null) {
                    engine = Arc.container().instance(Engine.class).get();
                }
            }
        }
        requestContext.setResult(createMulti(requestContext, (TemplateInstance) result));
    }

    private Multi<String> createMulti(ResteasyReactiveRequestContext requestContext, TemplateInstance templateInstance) {
        setSelectedVariant(templateInstance, requestContext.getRequest(),
                requestContext.getHttpHeaders().getAcceptableLanguages());
        return templateInstance.createMulti();
    }

}
