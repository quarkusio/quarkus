package io.quarkus.vertx.web.runtime;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public final class RouteHandlers {

    private RouteHandlers() {
    }

    static final String CONTENT_TYPE = "content-type";

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

}
