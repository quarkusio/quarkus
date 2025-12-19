package io.quarkus.vertx.http.runtime.filters;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;

public class GracefulShutdownFilter implements ShutdownListener, Handler<HttpServerRequest> {

    private static final Logger log = Logger.getLogger(GracefulShutdownFilter.class);

    private volatile Handler<HttpServerRequest> next;
    private volatile boolean running = true;
    private final AtomicInteger currentRequestCount = new AtomicInteger();
    private final AtomicReference<ShutdownNotification> notification = new AtomicReference<>();

    private final Handler<Void> requestDoneHandler = new Handler<>() {
        @Override
        public void handle(Void event) {
            int count = currentRequestCount.decrementAndGet();
            if (!running) {
                if (count == 0) {
                    ShutdownNotification n = notification.get();
                    if (n != null) {
                        if (notification.compareAndSet(n, null)) {
                            n.done();
                            log.info("All HTTP requests complete");
                        }
                    }
                }
            }
        }
    };

    @Override
    public void handle(HttpServerRequest event) {
        currentRequestCount.incrementAndGet();
        //todo: some way to do this without a wrapper solution
        ((QuarkusRequestWrapper) event).addRequestDoneHandler(requestDoneHandler);

        if (event.version() == HttpVersion.HTTP_1_1) {
            // For HTTP/1.1, add the header as otherwise the client will consider the connection to be keep-alive.
            // "Connection-specific header fields such as Connection and Keep-Alive are prohibited in HTTP/2 and HTTP/3"
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Connection
            // Therefore only add it for HTTP/1.1
            if (!running) {
                event.response().headers().add(HttpHeaderNames.CONNECTION, "close");
                ((QuarkusRequestWrapper) event).addRequestDoneHandler(unused -> event.connection().close());
            }
        } else if (event.version() == HttpVersion.HTTP_2) {
            if (!running) {
                // If shutdown is in progress, send the go away as early as possible
                sendGoAwayForHttp2(event.connection());
            } else {
                ((QuarkusRequestWrapper) event).addRequestDoneHandler(unused -> {
                    // Check again at the end of the request if we should send a shutdown
                    if (!running) {
                        sendGoAwayForHttp2(event.connection());
                    }
                });
            }
        }

        next.handle(event);
    }

    Set<Object> connectionsWithGoAway =
            Collections.synchronizedSet(
            Collections.newSetFromMap(
                    new WeakHashMap<>()
            ));

    private void sendGoAwayForHttp2(HttpConnection connection) {
        if (!connectionsWithGoAway.add(connection)) {
            return;
        }
        // GO_AWAY + 0 (NO_ERROR) = graceful shutdown; client will stop creating new streams on this connection
        connection.goAway(0);

        // Do not do a connection shutdown as OpenJDK 21.0.9 has problems and reports an "EOF reached while reading"
        // as all messages are processed asynchronously and that might take a while.
        // See Http2Connection.Http2TubeSubscriber#onComplete()
        // Problem persists in OpenJDK 25.0.1
        // A workaround could be to add a delay here, but for now no vertx instance is at hand to set a timer.
        // connection.shutdown();
    }

    @Override
    public void preShutdown(ShutdownNotification notification) {
        running = false;
        notification.done();
    }

    @Override
    public void shutdown(ShutdownNotification notification) {
        this.notification.set(notification);
        running = false;
        if (currentRequestCount.get() == 0) {
            if (this.notification.compareAndSet(notification, null)) {
                notification.done();
            }
        } else {
            log.info("Waiting for HTTP requests to complete");
        }
    }

    public void next(Handler<HttpServerRequest> next) {
        this.next = next;
    }

}
