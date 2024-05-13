package io.quarkus.resteasy.reactive.server.runtime.observability;

import java.util.Collections;
import java.util.List;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class ObservabilityCustomizer implements HandlerChainCustomizer {
    @Override
    public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
            ServerResourceMethod serverResourceMethod) {
        if (phase.equals(Phase.AFTER_MATCH)) {
            String basePath = resourceClass.getPath();
            boolean isSubResource = basePath == null;
            ObservabilityHandler observabilityHandler = new ObservabilityHandler();
            if (isSubResource) {
                observabilityHandler.setTemplatePath(serverResourceMethod.getPath());
                observabilityHandler.setSubResource(true);
            } else {
                observabilityHandler.setTemplatePath(basePath + serverResourceMethod.getPath());
                observabilityHandler.setSubResource(false);
            }

            return Collections.singletonList(observabilityHandler);
        }
        return Collections.emptyList();
    }
}
