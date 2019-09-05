package io.quarkus.vertx.web.deployment.devmode;

import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.deployment.devmode.ReplacementDebugPage;
import io.quarkus.vertx.web.runtime.VertxWebRecorder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class VertxHotReplacementSetup implements HotReplacementSetup {

    private volatile long nextUpdate;
    private HotReplacementContext hotReplacementContext;

    private static final long HOT_REPLACEMENT_INTERVAL = 2000;

    private static final String HEADER_NAME = "x-quarkus-hot-deployment-done";

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.hotReplacementContext = context;
        VertxWebRecorder.setHotReplacement(this::handleHotReplacementRequest);
    }

    @Override
    public void handleFailedInitialStart() {
        VertxWebRecorder.startServerAfterFailedStart();
    }

    void handleHotReplacementRequest(RoutingContext routingContext) {
        if ((nextUpdate > System.currentTimeMillis() && !hotReplacementContext.isTest())
                || routingContext.request().headers().contains(HEADER_NAME)) {
            if (hotReplacementContext.getDeploymentProblem() != null) {
                handleDeploymentProblem(routingContext, hotReplacementContext.getDeploymentProblem());
                return;
            }
            routingContext.next();
            return;
        }
        routingContext.request().pause();
        routingContext.vertx().executeBlocking(new Handler<Promise<Boolean>>() {
            @Override
            public void handle(Promise<Boolean> event) {
                boolean restart = false;
                synchronized (this) {
                    if (nextUpdate < System.currentTimeMillis() || hotReplacementContext.isTest()) {
                        try {
                            restart = hotReplacementContext.doScan(true);
                        } catch (Exception e) {
                            event.fail(new IllegalStateException("Unable to perform hot replacement scanning", e));
                            return;
                        }
                        nextUpdate = System.currentTimeMillis() + HOT_REPLACEMENT_INTERVAL;
                    }
                }
                if (hotReplacementContext.getDeploymentProblem() != null) {
                    event.fail(hotReplacementContext.getDeploymentProblem());
                    return;
                }
                event.complete(restart);
            }
        }, false, new Handler<AsyncResult<Boolean>>() {
            @Override
            public void handle(AsyncResult<Boolean> event) {
                if (!routingContext.request().isEnded()) {
                    routingContext.request().resume();
                }
                if (event.failed()) {
                    handleDeploymentProblem(routingContext, event.cause());
                } else {
                    boolean restart = event.result();
                    if (restart) {
                        routingContext.request().headers().set(HEADER_NAME, "true");
                        VertxWebRecorder.getRouter().handle(routingContext.request());
                    } else {
                        routingContext.next();
                    }
                }
            }
        });

    }

    public static void handleDeploymentProblem(RoutingContext routingContext, final Throwable exception) {
        String bodyText = ReplacementDebugPage.generateHtml(exception);
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(500);
        response.headers().add("Content-Type", "text/html; charset=UTF-8");
        response.end(bodyText);
    }

    @Override
    public void close() {
        VertxWebRecorder.shutDownDevMode();
    }
}
