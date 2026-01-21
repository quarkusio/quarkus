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
            /*
             * Only send GOAWAY and shutdown after completing the response, as the Vert.x 4.5.23 HttpClient does handle it
             * badly.
             * It doesn't proceed the last DATA frame well when a GOAWAY was sent.
             * This is fixed in upstream issue https://github.com/eclipse-vertx/vert.x/issues/5693
             */
            if (!running) {
                ((QuarkusRequestWrapper) event).addRequestDoneHandler(unused -> {
                    sendGoAwayForHttp2(event.connection(), event.getHeader(HttpHeaderNames.USER_AGENT));
                });
            }
        }

        next.handle(event);
    }

    private final Set<Object> connectionsWithGoAway = Collections.synchronizedSet(
            Collections.newSetFromMap(
                    new WeakHashMap<>()));

    private void sendGoAwayForHttp2(HttpConnection connection, String userAgent) {
        // First check if user agent supports GOAWAY. If we are connected to the client directly, we might trust it.
        // If there is a proxy in the middle, let's wait for the next request to check again.
        if (isAffectedByJDK8335181(userAgent)) {
            return;
        }

        // Avoid sending multiple GOAWAYs for a single connection to avoid confusing the caller
        if (!connectionsWithGoAway.add(connection)) {
            return;
        }

        // GO_AWAY + 0 (NO_ERROR) = graceful shutdown; client will stop creating new streams on this connection
        connection.goAway(0);

        // Do not do a connection shutdown as OpenJDK 21.0.9 has problems and reports an "EOF reached while reading"
        // when the connection close is received while messages are processed asynchronously which might take a while.
        // See Http2Connection.Http2TubeSubscriber#onComplete()
        // Problem persists in OpenJDK 25.0.1
        // A workaround could be to add a delay here, but for now no vertx instance is at hand to set a timer.
        // Tracked in upstream issue https://bugs.openjdk.org/browse/JDK-8374534
        if (userAgent != null && userAgent.startsWith("Java-http-client/")) {
            return;
        }

        connection.shutdown();
    }

    /**
     * Test if the client is affected by <a href="https://bugs.openjdk.org/browse/JDK-8335181">JDK-8335181</a>.
     * Affected clients do not support GOAWAY at all. Fixed in 21.0.8 and 17.0.17 which were released in mid-2025.
     *
     * @return true for all Java http clients that are known to be affected, and those where we can't parse the version header
     */
    protected static boolean isAffectedByJDK8335181(String userAgent) {
        if (userAgent == null || !userAgent.startsWith("Java-http-client/")) {
            return false;
        }

        String[] splitAgent = userAgent.split("/");
        if (splitAgent.length != 2) {
            return true;
        }
        String version = splitAgent[1];
        String[] splitVersion = version.split("\\.");
        if (splitVersion.length < 3) {
            return true;
        }
        try {
            int major = Integer.parseInt(splitVersion[0]);
            if (major >= 25) {
                return false;
            }
            int minor = Integer.parseInt(splitVersion[1]);
            int patch = Integer.parseInt(splitVersion[2]);
            if (major == 17 && minor == 0 && patch >= 17) {
                return false;
            }
            if (major == 21 && minor == 0 && patch >= 8) {
                return false;
            }
        } catch (NumberFormatException e) {
            return true;
        }
        return true;
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
