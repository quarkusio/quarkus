package io.quarkus.vertx.web.runtime;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public final class RouteHandlers {

    private RouteHandlers() {
    }

    public static void setContentType(RoutingContext context, String defaultContentType) {
        HttpServerResponse response = context.response();
        context.addHeadersEndHandler(new Handler<Void>() {
            @Override
            public void handle(Void aVoid) {
                var headers = response.headers();
                //use a listener to set the content type if it has not been set
                if (!headers.contains(CONTENT_TYPE)) {
                    String acceptableContentType = context.getAcceptableContentType();
                    // we can use add because we know already there's no content type
                    if (acceptableContentType != null) {
                        headers.add(CONTENT_TYPE, acceptableContentType);
                    } else if (defaultContentType != null) {
                        headers.add(CONTENT_TYPE, defaultContentType);
                    }
                }
            }
        });
    }

}
