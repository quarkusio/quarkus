package io.quarkus.vertx.web.runtime;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import tools.jackson.databind.exc.MismatchedInputException;

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

    public static JsonObject bodyAsJsonObject(RequestBody body) {
        try {
            return body.asJsonObject();
        } catch (DecodeException | ClassCastException e) {
            throw new HttpException(400, e);
        }
    }

    public static JsonArray bodyAsJsonArray(RequestBody body) {
        try {
            return body.asJsonArray();
        } catch (DecodeException | ClassCastException e) {
            throw new HttpException(400, e);
        }
    }

    public static Object bodyMapTo(JsonObject body, Class<?> type) {
        try {
            return body.mapTo(type);
        } catch (MismatchedInputException e) {
            throw new HttpException(400, e);
        }
    }
}
