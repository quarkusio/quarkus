package io.quarkus.vertx.http.runtime.devmode;

import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.dev.ErrorPageGenerators;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpHotReplacementSetup implements HotReplacementSetup {

    private volatile long nextUpdate;
    private HotReplacementContext hotReplacementContext;

    private static final long HOT_REPLACEMENT_INTERVAL = 2000;

    private static final String HEADER_NAME = "x-quarkus-hot-deployment-done";

    private static final String CONFIG_FIX = "io.quarkus.vertx-http.devmode.config.fix";

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        // ensure that Vert.x runs in dev mode, this prevents Vert.x from caching static resources
        System.setProperty("vertxweb.environment", "dev");
        this.hotReplacementContext = context;
        VertxHttpRecorder.setHotReplacement(this::handleHotReplacementRequest, hotReplacementContext);
        hotReplacementContext.addPreScanStep(new Runnable() {
            @Override
            public void run() {
                RemoteSyncHandler.doPreScan();
            }
        });
    }

    @Override
    public void handleFailedInitialStart() {
        VertxHttpRecorder.startServerAfterFailedStart();
    }

    void handleHotReplacementRequest(RoutingContext routingContext) {
        if (hotReplacementContext.getDeploymentProblem() != null && routingContext.request().path().endsWith(CONFIG_FIX)) {

            routingContext.request().setExpectMultipart(true);
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    try {
                        String redirect = "/";
                        MultiMap attrs = routingContext.request().formAttributes();
                        Map<String, String> newVals = new HashMap<>();
                        for (Map.Entry<String, String> i : attrs) {
                            if (i.getKey().startsWith("key.")) {
                                newVals.put(i.getKey().substring("key.".length()), i.getValue());
                            } else if (i.getKey().equals("redirect")) {
                                redirect = i.getValue();
                            }
                        }
                        CurrentConfig.EDITOR.accept(newVals);
                        routingContext.response().setStatusCode(HttpResponseStatus.SEE_OTHER.code()).headers()
                                .set(HttpHeaderNames.LOCATION, redirect);
                        routingContext.response().end();
                    } catch (Throwable t) {
                        routingContext.fail(t);
                    }
                }
            });
            routingContext.request().resume();
            return;
        }
        if ((nextUpdate > System.currentTimeMillis() && !hotReplacementContext.isTest())
                || routingContext.request().headers().contains(HEADER_NAME)) {
            if (hotReplacementContext.getDeploymentProblem() != null) {
                handleDeploymentProblem(routingContext, hotReplacementContext.getDeploymentProblem());
                return;
            }
            routingContext.next();
            return;
        }
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        ConnectionBase connectionBase = (ConnectionBase) routingContext.request().connection();
        connectionBase.getContext().executeBlocking(new Handler<Promise<Boolean>>() {
            @Override
            public void handle(Promise<Boolean> event) {
                //the blocking pool may have a stale TCCL
                Thread.currentThread().setContextClassLoader(current);
                boolean restart = false;
                synchronized (this) {
                    if (nextUpdate < System.currentTimeMillis() || hotReplacementContext.isTest()) {
                        nextUpdate = System.currentTimeMillis() + HOT_REPLACEMENT_INTERVAL;
                        try {
                            restart = hotReplacementContext.doScan(true);
                        } catch (Exception e) {
                            event.fail(new IllegalStateException("Unable to perform live reload scanning", e));
                            return;
                        }
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
                if (event.failed()) {
                    handleDeploymentProblem(routingContext, event.cause());
                } else {
                    boolean restart = event.result();
                    if (restart) {
                        routingContext.request().headers().set(HEADER_NAME, "true");
                        VertxHttpRecorder.getRootHandler().handle(routingContext.request());
                    } else {
                        routingContext.next();
                    }
                }
            }
        });

    }

    public static void handleDeploymentProblem(RoutingContext routingContext, final Throwable exception) {
        String bodyText = ReplacementDebugPage.generateHtml(exception, routingContext.request().absoluteURI());
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(500);
        response.headers().add("Content-Type", "text/html; charset=UTF-8");
        response.end(bodyText);
    }

    @Override
    public void close() {
        ErrorPageGenerators.clear();
        VertxHttpRecorder.shutDownDevMode();
    }
}
