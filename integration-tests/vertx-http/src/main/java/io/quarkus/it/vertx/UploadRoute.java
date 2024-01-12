package io.quarkus.it.vertx;

import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.http.runtime.RouteConstants;
import io.quarkus.vertx.http.runtime.ServerLimitsConfig;
import io.quarkus.vertx.http.runtime.options.HttpServerCommonHandlers;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class UploadRoute {

    /**
     * Installs two POST-routes - one that bypasses the body-length limit using {@code order(-3)}
     * ({@link HttpServerCommonHandlers#enforceMaxBodySize(ServerLimitsConfig, Router)} uses
     * {@value RouteConstants#ROUTE_ORDER_UPLOAD_LIMIT} for {@link io.vertx.ext.web.Route#order(int)}) and one that
     * does not bypass body-size enforcement.
     */
    void installRoute(@Observes StartupEvent startupEvent, Router router) {
        router.post("/unlimited-upload").order(RouteConstants.ROUTE_ORDER_UPLOAD_LIMIT - 1).handler(UploadHandler::newRequest);
        router.post("/limited-upload").handler(UploadHandler::newRequest);
    }

    static class UploadHandler {
        final HttpServerResponse resp;

        final AtomicLong total = new AtomicLong();

        UploadHandler(HttpServerResponse resp) {
            this.resp = resp;
        }

        void end(Void x) {
            resp.setStatusCode(200)
                    .setStatusMessage("OK")
                    .putHeader("Content-Type", "text/plain")
                    .end("Got " + total);
        }

        void onData(Buffer buffer) {
            total.addAndGet(buffer.length());
        }

        void onException(Throwable exception) {
            resp.setStatusCode(500)
                    .setStatusMessage("Internal Server Error")
                    .end("Failed to process request.");
        }

        static void newRequest(RoutingContext routingContext) {
            HttpServerRequest req = routingContext.request();
            HttpServerResponse resp = routingContext.response();

            String expectValue = req.getHeader(HttpHeaders.EXPECT);
            if (expectValue != null) {
                if (!"100-continue".equals(expectValue)) {
                    routingContext.fail(417);
                }
                if (req.version() != HttpVersion.HTTP_1_0) {
                    resp.writeContinue();
                }
            }

            UploadHandler uploadHandler = new UploadHandler(resp);
            req.handler(uploadHandler::onData)
                    .endHandler(uploadHandler::end)
                    .exceptionHandler(uploadHandler::onException);
        }
    }
}
