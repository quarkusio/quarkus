package io.quarkus.vertx.http.runtime.filters;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public class GracefulShutdownFilter implements ShutdownListener, Handler<HttpServerRequest> {

    private static Logger log = Logger.getLogger(GracefulShutdownFilter.class);

    private volatile Handler<HttpServerRequest> next;
    private volatile boolean running = true;
    private final AtomicInteger currentRequestCount = new AtomicInteger();
    private final AtomicReference<ShutdownNotification> notification = new AtomicReference<>();

    private final Handler<Void> requestDoneHandler = new Handler<Void>() {
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
        if (!running) {
            event.response().setStatusCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code())
                    .putHeader(HttpHeaderNames.CONNECTION, "close").end();
            return;
        }
        currentRequestCount.incrementAndGet();
        //todo: some way to do this without a wrapper solution
        ((QuarkusRequestWrapper) event).addRequestDoneHandler(requestDoneHandler);
        next.handle(event);
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
