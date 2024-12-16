package io.quarkus.jfr.runtime.http.rest.reactive;

import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;
import io.quarkus.jfr.runtime.http.rest.RestStartEvent;

/**
 * Kicks off the creation of a {@link RestStartEvent}.
 * This is done very early as to be able to capture events such as 405, 406, etc.
 */
public class ServerStartRecordingHandler implements ServerRestHandler {

    private static final ServerStartRecordingHandler INSTANCE = new ServerStartRecordingHandler();

    private static final Logger LOG = Logger.getLogger(ServerStartRecordingHandler.class);

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Enter Jfr Reactive Request Filter");
        }
        requestContext.requireCDIRequestScope();
        ReactiveServerRecorder recorder = Arc.container().instance(ReactiveServerRecorder.class).get();
        recorder
                .createStartEvent()
                .createAndStartPeriodEvent();
    }

    public static class Customizer implements HandlerChainCustomizer {
        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_PRE_MATCH) {
                return Collections.singletonList(INSTANCE);
            }
            return Collections.emptyList();
        }
    }
}
