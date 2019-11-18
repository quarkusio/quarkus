package io.quarkus.vertx.http.runtime;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;

import org.jboss.logging.Logger;

import io.vertx.ext.web.RoutingContext;

@RequestScoped
public class CurrentVertxRequest {

    private final Logger log = Logger.getLogger(CurrentVertxRequest.class);

    public RoutingContext current;
    private List<Listener> doneListeners;

    @Produces
    @RequestScoped
    public RoutingContext getCurrent() {
        return current;
    }

    public CurrentVertxRequest setCurrent(RoutingContext current) {
        this.current = current;
        return this;
    }

    public void addRequestDoneListener(Listener doneListener) {
        if (doneListeners == null) {
            doneListeners = new ArrayList<>();
        }
        doneListeners.add(doneListener);
    }

    public void initialInvocationComplete(boolean goingAsync) {

        if (current == null) {
            return;
        }
        if (doneListeners != null) {
            for (Listener i : doneListeners) {
                try {
                    i.initialInvocationComplete(current, goingAsync);
                } catch (Throwable t) {
                    log.errorf(t, "Failed to process invocation listener %s", i);
                }
            }
        }
    }

    @PreDestroy
    void done() {
        if (current == null) {
            return;
        }
        if (doneListeners != null) {
            for (Listener i : doneListeners) {
                try {
                    i.responseComplete(current);
                } catch (Throwable t) {
                    log.errorf(t, "Failed to process done listener %s", i);
                }
            }
        }
    }

    public interface Listener {

        void initialInvocationComplete(RoutingContext routingContext, boolean goingAsync);

        void responseComplete(RoutingContext routingContext);

    }

}
