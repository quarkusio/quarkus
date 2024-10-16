package io.quarkus.vertx.http.runtime.devmode;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.dev.ErrorPageGenerators;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.vertx.core.runtime.QuarkusExecutorFactory;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.NoStackTraceException;
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
        hotReplacementContext.addPostRestartStep(new Runnable() {
            @Override
            public void run() {
                // If not on a worker thread then attempt to re-initialize the dev mode executor
                if (!Context.isOnWorkerThread()) {
                    QuarkusExecutorFactory.reinitializeDevModeExecutor();
                }
            }
        });
    }

    @Override
    public void handleFailedInitialStart() {
        //remove for vert.x 4.2
        //at the moment there is a TCCL error that is normally handled by the log filters
        //but if startup fails it may not take effect
        //it happens once per thread, so it can completely mess up the console output, and hide the real issue
        LogManager.getLogManager().getLogger("io.vertx.core.impl.ContextImpl").setLevel(Level.SEVERE);
        VertxHttpRecorder.startServerAfterFailedStart();
    }

    private static volatile Set<ConnectionBase> openConnections;

    public static void handleDevModeRestart() {
        if (DevConsoleManager.isDoingHttpInitiatedReload()) {
            return;
        }
        Set<ConnectionBase> cons = VertxHttpHotReplacementSetup.openConnections;
        if (cons != null) {
            for (ConnectionBase con : cons) {
                con.close();
            }
        }
    }

    void handleHotReplacementRequest(RoutingContext routingContext) {
        if (openConnections == null) {
            synchronized (VertxHttpHotReplacementSetup.class) {
                if (openConnections == null) {
                    openConnections = Collections.newSetFromMap(new ConcurrentHashMap<>());
                }
            }
        }
        ConnectionBase connectionBase = (ConnectionBase) routingContext.request().connection();
        if (openConnections.add(connectionBase)) {
            connectionBase.closeFuture().onComplete(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> event) {
                    openConnections.remove(connectionBase);
                }
            });
        }
        if (hotReplacementContext.getDeploymentProblem() != null && routingContext.request().path().endsWith(CONFIG_FIX)) {

            routingContext.request().setExpectMultipart(true);
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    VertxCoreRecorder.getVertx().get().getOrCreateContext().executeBlocking(new Callable<Void>() {
                        @Override
                        public Void call() {
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
                            return null;
                        }
                    }).onFailure(routingContext::fail);
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
        VertxCoreRecorder.getVertx().get().getOrCreateContext().executeBlocking(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                //the blocking pool may have a stale TCCL
                Thread.currentThread().setContextClassLoader(current);
                boolean restart = false;
                try {
                    DevConsoleManager.setDoingHttpInitiatedReload(true);
                    synchronized (this) {
                        if (nextUpdate < System.currentTimeMillis() || hotReplacementContext.isTest()) {
                            nextUpdate = System.currentTimeMillis() + HOT_REPLACEMENT_INTERVAL;
                            Object currentState = VertxHttpRecorder.getCurrentApplicationState();
                            try {
                                restart = hotReplacementContext.doScan(true);
                            } catch (Exception e) {
                                throw new IllegalStateException("Unable to perform live reload scanning", e);
                            }
                            if (currentState != VertxHttpRecorder.getCurrentApplicationState()) {
                                //its possible a Kafka message or some other source triggered a reload,
                                //so we could wait for the restart (due to the scan lock)
                                //but then fail to dispatch to the new application
                                restart = true;
                            }
                        }
                    }
                    if (hotReplacementContext.getDeploymentProblem() != null) {
                        throw new NoStackTraceException(hotReplacementContext.getDeploymentProblem());
                    }
                    if (restart) {
                        //close all connections on close, except for this one
                        //this prevents long-running requests such as SSE or websockets
                        //from holding onto the old deployment
                        Set<ConnectionBase> connections = new HashSet<>(openConnections);
                        for (ConnectionBase con : connections) {
                            if (con != connectionBase) {
                                con.close();
                            }
                        }
                    }
                } finally {
                    DevConsoleManager.setDoingHttpInitiatedReload(false);
                }
                return restart;
            }
        }, false).onComplete(new Handler<AsyncResult<Boolean>>() {
            @Override
            public void handle(AsyncResult<Boolean> event) {
                if (event.failed()) {
                    handleDeploymentProblem(routingContext, event.cause());
                } else {
                    boolean restart = event.result();
                    if (restart) {
                        QuarkusExecutorFactory.reinitializeDevModeExecutor();
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
