package io.quarkus.arc.impl;

import io.quarkus.arc.AsyncObserverExceptionHandler;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

@DefaultBean
@Singleton
public class DefaultAsyncObserverExceptionHandler implements AsyncObserverExceptionHandler {

    private static final Logger LOG = Logger.getLogger(DefaultAsyncObserverExceptionHandler.class);

    @Override
    public void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext) {
        LOG.errorf(
                "Failure occurred while notifying an async %s for event of type %s \n- please enable debug logging to see the full stack trace",
                observerMethod, eventContext.getMetadata().getType().getTypeName());
        LOG.debugf(throwable, "Failure occurred while notifying an async %s for event of type %s",
                observerMethod, eventContext.getMetadata().getType().getTypeName());
    }

}
