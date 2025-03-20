package io.quarkus.arc.impl;

import java.lang.reflect.Type;

import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.AsyncObserverExceptionHandler;
import io.quarkus.arc.DefaultBean;

@DefaultBean
@Singleton
public class DefaultAsyncObserverExceptionHandler implements AsyncObserverExceptionHandler {

    private static final Logger LOG = Logger.getLogger(DefaultAsyncObserverExceptionHandler.class);

    @Override
    public void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext) {
        Type type = eventContext.getMetadata().getType();
        LOG.errorf(
                "Failure occurred while notifying an async %s for event of type %s \n- please enable debug logging (for example using %s) to see the full stack trace",
                observerMethod, type.getTypeName(), loggingProperty(type));
        LOG.debugf(throwable, "Failure occurred while notifying an async %s for event of type %s",
                observerMethod, type.getTypeName());
    }

    private String loggingProperty(Type type) {
        var dotIndex = type.getTypeName().lastIndexOf('.');
        String packageName = dotIndex == -1 ? "" : type.getTypeName().substring(0, dotIndex);
        if (packageName.isEmpty()) {
            return "quarkus.log.level=DEBUG";
        } else {
            return "'quarkus.log.category.\"%s\".level=DEBUG'";
        }
    }
}
