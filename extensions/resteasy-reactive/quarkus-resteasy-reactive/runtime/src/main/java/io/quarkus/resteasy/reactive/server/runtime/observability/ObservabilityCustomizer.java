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
            ObservabilityHandler observabilityHandler = new ObservabilityHandler();
            observabilityHandler
                    .setTemplatePath(resourceClass.getPath() + serverResourceMethod.getPath());
            return Collections.singletonList(observabilityHandler);
        }
        return Collections.emptyList();
    }
}
