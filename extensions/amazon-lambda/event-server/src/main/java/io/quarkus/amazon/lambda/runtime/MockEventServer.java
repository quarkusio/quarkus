package io.quarkus.amazon.lambda.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MockEventServer implements Closeable {
    protected static final Logger log = Logger.getLogger(MockEventServer.class);
    public static final int DEFAULT_PORT = 8081;

    public void start() {
        int port = DEFAULT_PORT;
        start(port);
    }

    public void start(int port) {
        this.port = port;
        vertx = Vertx.vertx();
        httpServer = vertx.createHttpServer();
        router = Router.router(vertx);
        setupRoutes();
        httpServer.requestHandler(router).listen(port).result();
        log.info("Mock Lambda Event Server Started");
    }

    private Vertx vertx;
    private int port;
    protected HttpServer httpServer;
    protected Router router;
    protected BlockingQueue<RoutingContext> queue;
    protected ConcurrentHashMap<String, RoutingContext> responsePending = new ConcurrentHashMap<>();
    protected ExecutorService blockingPool = Executors.newCachedThreadPool();
    public static final String BASE_PATH = AmazonLambdaApi.API_BASE_PATH_TEST;
    public static final String INVOCATION = BASE_PATH + AmazonLambdaApi.API_PATH_INVOCATION;
    public static final String NEXT_INVOCATION = BASE_PATH + AmazonLambdaApi.API_PATH_INVOCATION_NEXT;
    public static final String POST_EVENT = BASE_PATH;

    public MockEventServer() {
        queue = new LinkedBlockingQueue<>();
    }

    public HttpServer getHttpServer() {
        return httpServer;
    }

    public void setupRoutes() {
        router.route().handler(BodyHandler.create());
        router.post(POST_EVENT).handler(this::postEvent);
        router.route(NEXT_INVOCATION).blockingHandler(this::nextEvent);
        router.route(INVOCATION + ":requestId" + AmazonLambdaApi.API_PATH_REQUEUE).handler(this::handleRequeue);
        router.route(INVOCATION + ":requestId" + AmazonLambdaApi.API_PATH_RESPONSE).handler(this::handleResponse);
        router.route(INVOCATION + ":requestId" + AmazonLambdaApi.API_PATH_ERROR).handler(this::handleError);
        defaultHanderSetup();
    }

    protected void defaultHanderSetup() {
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
            queue.put(ctx);
        } catch (InterruptedException e) {
            log.error("Publish interrupted");
            ctx.fail(500);
        }
    }

    private RoutingContext pollNextEvent() throws InterruptedException {
        for (;;) {
            RoutingContext request = queue.poll(10, TimeUnit.MILLISECONDS);
            if (request != null)
                return request;

        }
    }

    public void nextEvent(RoutingContext ctx) {
        // Vert.x barfs if you block too long so we have our own executor
        blockingPool.execute(() -> {
            final AtomicBoolean closed = new AtomicBoolean(false);
            ctx.response().closeHandler((v) -> closed.set(true));
            RoutingContext request = null;
            try {
                for (;;) {
                    request = queue.poll(10, TimeUnit.MILLISECONDS);
                    if (request != null) {
                        if (closed.get()) {
                            queue.put(request);
                            return;
                        } else {
                            break;
                        }
                    } else if (closed.get()) {
                        return;
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
            responsePending.put(requestId, request);
            ctx.response().putHeader(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID, requestId);
            Buffer body = processEventBody(request);
            if (body != null) {
                ctx.response().setStatusCode(200).end(body);
            } else {
                ctx.response().setStatusCode(200).end();
            }
        });
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
        log.info("Stopping Mock Lambda Event Server");
        httpServer.close().result();
        vertx.close().result();
        blockingPool.shutdown();
    }
}
