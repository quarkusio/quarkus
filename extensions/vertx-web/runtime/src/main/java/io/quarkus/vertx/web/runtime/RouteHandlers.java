package io.quarkus.vertx.web.runtime;

import javax.enterprise.event.Event;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public final class RouteHandlers {

    private RouteHandlers() {
    }

    static final String CONTENT_TYPE = "content-type";

    private static final LazyValue<Event<SecurityIdentity>> SECURITY_IDENTITY_EVENT = new LazyValue<>(
            RouteHandlers::createEvent);

    public static void setContentType(RoutingContext context, String defaultContentType) {
        HttpServerResponse response = context.response();
        context.addHeadersEndHandler(new Handler<Void>() {
            @Override
            public void handle(Void aVoid) {
                //use a listener to set the content type if it has not been set
                if (response.headers().get(CONTENT_TYPE) == null) {
                    String acceptableContentType = context.getAcceptableContentType();
                    if (acceptableContentType != null) {
                        response.putHeader(CONTENT_TYPE, acceptableContentType);
                    } else if (defaultContentType != null) {
                        response.putHeader(CONTENT_TYPE, defaultContentType);
                    }
                }
            }
        });
    }

    static void fireSecurityIdentity(SecurityIdentity identity) {
        SECURITY_IDENTITY_EVENT.get().fire(identity);
    }

    static void clear() {
        SECURITY_IDENTITY_EVENT.clear();
    }

    private static Event<SecurityIdentity> createEvent() {
        return Arc.container().beanManager().getEvent().select(SecurityIdentity.class);
    }

}
