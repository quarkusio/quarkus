package io.quarkus.amazon.lambda.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.runtime.configuration.MemorySize;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MockEventServer implements Closeable {
    protected static final Logger log = Logger.getLogger(MockEventServer.class);
    public static final int DEFAULT_PORT = 8081;

    private Vertx vertx;
    protected HttpServer httpServer;
    protected Router router;
    protected BlockingQueue<RoutingContext> queue;
    protected ConcurrentHashMap<String, RoutingContext> responsePending = new ConcurrentHashMap<>();
    protected ExecutorService blockingPool = Executors.newCachedThreadPool();
    public static final String BASE_PATH = AmazonLambdaApi.API_BASE_PATH_TEST;
    public static final String INVOCATION = BASE_PATH + AmazonLambdaApi.API_PATH_INVOCATION;
    public static final String NEXT_INVOCATION = BASE_PATH + AmazonLambdaApi.API_PATH_INVOCATION_NEXT;
    public static final String POST_EVENT = BASE_PATH;
    public static final String CONTINUE = "100-continue";
    private static final Set<String> COMMA_VALUE_HEADERS;

    static {
        COMMA_VALUE_HEADERS = new HashSet<>();
        COMMA_VALUE_HEADERS.add("date");
        COMMA_VALUE_HEADERS.add("last-modified");
        COMMA_VALUE_HEADERS.add("expires");
        COMMA_VALUE_HEADERS.add("if-modified-since");
        COMMA_VALUE_HEADERS.add("if-unmodified-since");
    }

    public static boolean canHaveCommaValue(String header) {
        return COMMA_VALUE_HEADERS.contains(header.toLowerCase(Locale.ROOT));
    }

    final AtomicBoolean closed = new AtomicBoolean();

    public MockEventServer() {
        queue = new LinkedBlockingQueue<>();
    }

    public void start() {
        start(DEFAULT_PORT);
    }

    public void start(int port) {
        vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(60).setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES));
        HttpServerOptions options = new HttpServerOptions();
        options.setPort(port == 0 ? -1 : port);

        Optional<MemorySize> maybeMaxHeadersSize = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.http.limits.max-header-size", MemorySize.class);

        if (maybeMaxHeadersSize.isPresent()) {
            options.setMaxHeaderSize(maybeMaxHeadersSize.get().asBigInteger().intValueExact());
        }

        httpServer = vertx.createHttpServer(options);
        router = Router.router(vertx);
        setupRoutes();
        try {
            this.httpServer.requestHandler(router).listen().toCompletionStage().toCompletableFuture().get();
            log.info("Mock Lambda Event Server Started");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPort() {
        return httpServer.actualPort();
    }

    public void setupRoutes() {
        router.route().handler((context) -> {
            if (context.get("continue-sent") == null) {
                String expect = context.request().getHeader(HttpHeaderNames.EXPECT);
                if (expect != null && expect.equalsIgnoreCase(CONTINUE)) {
                    context.put("continue-sent", true);
                    context.response().writeContinue();
                }
            }
            context.next();
        });
        router.route().handler(new MockBodyHandler());
        router.post(POST_EVENT).handler(this::postEvent);
        router.route(NEXT_INVOCATION).blockingHandler(this::nextEvent);
        router.route(INVOCATION + ":requestId" + AmazonLambdaApi.API_PATH_REQUEUE).handler(this::handleRequeue);
        router.route(INVOCATION + ":requestId" + AmazonLambdaApi.API_PATH_RESPONSE).handler(this::handleResponse);
        router.route(INVOCATION + ":requestId" + AmazonLambdaApi.API_PATH_ERROR).handler(this::handleError);
        defaultHandlerSetup();
    }

    protected void defaultHandlerSetup() {
        router.post().handler(this::postEvent);
    }

    public void postEvent(RoutingContext ctx) {
        String requestId = ctx.request().getHeader(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        ctx.put(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID, requestId);
        String traceId = ctx.request().getHeader(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        ctx.put(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY, traceId);
        try {
            log.debugf("Putting message %s into the queue", requestId);
            queue.put(ctx);
        } catch (InterruptedException e) {
            log.error("Publish interrupted");
            ctx.fail(500);
        }
    }

    public void nextEvent(RoutingContext ctx) {
        vertx.executeBlocking(new Callable<Void>() {
            @Override
            public Void call() {
                final AtomicBoolean closed = new AtomicBoolean(false);
                ctx.response().closeHandler((v) -> closed.set(true));
                ctx.response().exceptionHandler((v) -> closed.set(true));
                ctx.request().connection().closeHandler((v) -> closed.set(true));
                ctx.request().connection().exceptionHandler((v) -> closed.set(true));
                RoutingContext request = null;
                try {
                    for (;;) {
                        request = queue.poll(10, TimeUnit.MILLISECONDS);
                        if (request != null) {
                            if (closed.get()) {
                                log.debugf("Polled message %s but connection was closed, returning to queue",
                                        request.get(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID));
                                queue.put(request);
                                return null;
                            } else {
                                break;
                            }
                        } else if (closed.get()) {
                            return null;
                        }
                    }
                } catch (InterruptedException e) {
                    log.error("nextEvent interrupted");
                    ctx.fail(500);
                }

                String contentType = getEventContentType(request);
                if (contentType != null) {
                    ctx.response().putHeader("content-type", contentType);
                }
                String traceId = request.get(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY);
                if (traceId != null) {
                    ctx.response().putHeader(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY, traceId);
                }
                String requestId = request.get(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
                log.debugf("Starting processing %s, added to pending request map", requestId);
                responsePending.put(requestId, request);
                ctx.response().putHeader(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID, requestId);
                Buffer body = processEventBody(request);
                if (body != null) {
                    ctx.response().setStatusCode(200).end(body);
                } else {
                    ctx.response().setStatusCode(200).end();
                }
                return null;
            }
        }, false);
    }

    protected String getEventContentType(RoutingContext request) {
        return request.request().getHeader("content-type");
    }

    protected Buffer processEventBody(RoutingContext request) {
        return request.getBody();
    }

    public void handleResponse(RoutingContext ctx) {
        String requestId = ctx.pathParam("requestId");
        RoutingContext pending = responsePending.remove(requestId);
        if (pending == null) {
            log.error("Unknown lambda request: " + requestId);
            ctx.fail(404);
            return;
        }
        log.debugf("Sending response %s", requestId);
        Buffer buffer = ctx.getBody();
        processResponse(ctx, pending, buffer);
        ctx.response().setStatusCode(204);
        ctx.end();
    }

    public void handleRequeue(RoutingContext ctx) {
        String requestId = ctx.pathParam("requestId");
        RoutingContext pending = responsePending.remove(requestId);
        if (pending == null) {
            log.error("Unknown lambda request: " + requestId);
            ctx.fail(404);
            return;
        }
        log.debugf("Requeue %s", requestId);
        try {
            queue.put(pending);
        } catch (InterruptedException e) {
            log.error("Publish interrupted");
            ctx.fail(500);
        }
        ctx.response().setStatusCode(204);
        ctx.end();
    }

    public void processResponse(RoutingContext ctx, RoutingContext pending, Buffer buffer) {
        if (buffer != null) {
            if (ctx.request().getHeader("Content-Type") != null) {
                pending.response().putHeader("Content-Type", ctx.request().getHeader("Content-Type"));
            }
            pending.response()
                    .setStatusCode(200)
                    .end(buffer);
        } else {
            pending.response()
                    .setStatusCode(204)
                    .end();
        }
    }

    public void handleError(RoutingContext ctx) {
        String requestId = ctx.pathParam("requestId");
        RoutingContext pending = responsePending.remove(requestId);
        if (pending == null) {
            log.error("Unknown lambda request: " + requestId);
            ctx.fail(404);
            return;
        }
        log.debugf("Sending response %s", requestId);
        Buffer buffer = ctx.getBody();
        processError(ctx, pending, buffer);
        ctx.response().setStatusCode(204);
        ctx.end();
    }

    public void processError(RoutingContext ctx, RoutingContext pending, Buffer buffer) {
        if (buffer != null) {
            if (ctx.request().getHeader("Content-Type") != null) {
                pending.response().putHeader("Content-Type", ctx.request().getHeader("Content-Type"));
            }
            pending.response()
                    .setStatusCode(500)
                    .end(buffer);
        } else {
            pending.response()
                    .setStatusCode(500)
                    .end();
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        log.info("Stopping Mock Lambda Event Server");
        for (var i : responsePending.entrySet()) {
            i.getValue().response().setStatusCode(503).end();
        }
        for (var i : queue) {
            i.response().setStatusCode(503).end();
        }
        try {
            httpServer.close().toCompletionStage().toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                vertx.close().toCompletionStage().toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            } finally {
                blockingPool.shutdown();
            }
        }
    }
}
