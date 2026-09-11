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
    private volatile Throwable tempDeploymentProblem;
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
        hotReplacementContext.addPreRestartStep(new Runnable() {
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

    /**
     * Connections with a request waiting in {@link #handleHotReplacementRequest} for a scan or a restart to complete.
     * They are not closed when the application stops: the request is dispatched to the restarted application once it
     * is up.
     */
    private static final Set<ConnectionBase> waitingConnections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * The application state at the time the application was last shut down for a restart. Until the restarted
     * application has installed its root handler, the state is still this one and requests must not be dispatched
     * to the handlers of the stopped application.
     */
    private static volatile Object restartingFrom;

    public static void handleDevModeRestart() {
        restartingFrom = VertxHttpRecorder.getCurrentApplicationState();
        if (DevConsoleManager.isDoingHttpInitiatedReload()) {
            return;
        }
        Set<ConnectionBase> cons = VertxHttpHotReplacementSetup.openConnections;
        if (cons != null) {
            for (ConnectionBase con : cons) {
                if (!waitingConnections.contains(con)) {
                    con.close();
                }
            }
        }
    }

    /**
     * Whether a restart that was not initiated by an HTTP request, for example one triggered by an extension watching
     * for changes, is in progress.
     */
    private static boolean isRestarting() {
        Object from = restartingFrom;
        return from != null && from == VertxHttpRecorder.getCurrentApplicationState();
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
        if (tempDeploymentProblem != null) {
            handleDeploymentProblem(routingContext, tempDeploymentProblem);
            return;
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
                    }, false).onFailure(routingContext::fail);
                }
            });
            routingContext.request().resume();
            return;
        }
        if ((nextUpdate > System.currentTimeMillis() &&
                !hotReplacementContext.isTest() &&
                !DevConsoleManager.isDoingHttpInitiatedReload() // if there is a live reload possibly going on we don't want to let a request through to restarting application, this is best effort, but it narrows the window a lot
                && !isRestarting())
                || routingContext.request().headers().contains(HEADER_NAME)) {
            if (hotReplacementContext.getDeploymentProblem() != null) {
                handleDeploymentProblem(routingContext, hotReplacementContext.getDeploymentProblem());
                return;
            }
            routingContext.next();
            return;
        }
        // We need to set the flag immediately after the check to mitigate
        // the timing issue when multiple requests are processed concurrently
        DevConsoleManager.setDoingHttpInitiatedReload(true);
        waitingConnections.add(connectionBase);
        try {
            ClassLoader current = Thread.currentThread().getContextClassLoader();
            VertxCoreRecorder.getVertx().get().getOrCreateContext().executeBlocking(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    //the blocking pool may have a stale TCCL
                    Thread.currentThread().setContextClassLoader(current);
                    boolean restart = false;
                    Object currentState = VertxHttpRecorder.getCurrentApplicationState();
                    synchronized (VertxHttpHotReplacementSetup.this) {
                        // a restart triggered by another source is waited for through the scan lock, whatever the
                        // time of the last scan
                        if (nextUpdate < System.currentTimeMillis() || hotReplacementContext.isTest() || isRestarting()) {
                            nextUpdate = System.currentTimeMillis() + HOT_REPLACEMENT_INTERVAL;
                            try {
                                tempDeploymentProblem = hotReplacementContext.getDeploymentProblem();
                                restart = hotReplacementContext.doScan(true);
                            } catch (Exception e) {
                                throw new IllegalStateException("Unable to perform live reload scanning", e);
                            } finally {
                                tempDeploymentProblem = null;
                            }
                        }
                    }
                    if (currentState != VertxHttpRecorder.getCurrentApplicationState()) {
                        //its possible a Kafka message or some other source triggered a reload,
                        //so we could wait for the restart (due to the scan lock)
                        //but then fail to dispatch to the new application
                        restart = true;
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
                            if (con != connectionBase && !waitingConnections.contains(con)) {
                                con.close();
                            }
                        }
                    }
                    return restart;
                }
            }, false).onComplete(new Handler<AsyncResult<Boolean>>() {
                @Override
                public void handle(AsyncResult<Boolean> result) {
                    // Unset the flag when the blocking code completes
                    DevConsoleManager.setDoingHttpInitiatedReload(false);
                    waitingConnections.remove(connectionBase);
                    if (result.failed()) {
                        handleDeploymentProblem(routingContext, result.cause());
                    } else {
                        boolean restart = result.result();
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
        } catch (Throwable e) {
            // Make sure the flag is unset when something bad happens
            DevConsoleManager.setDoingHttpInitiatedReload(false);
            waitingConnections.remove(connectionBase);
            throw e;
        }

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
