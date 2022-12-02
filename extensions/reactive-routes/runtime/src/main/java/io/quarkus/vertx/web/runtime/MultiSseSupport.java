package io.quarkus.vertx.web.runtime;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.quarkus.vertx.web.ReactiveRoutes;
import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

@SuppressWarnings("ReactiveStreamsSubscriberImplementation")
public class MultiSseSupport {

    private MultiSseSupport() {
        // Avoid direct instantiation.
    }

    public static void subscribeString(Multi<String> multi, RoutingContext rc) {
        subscribeBuffer(multi.map(new Function<String, Buffer>() {
            @Override
            public Buffer apply(String s) {
                return Buffer.buffer(s);
            }
        }), rc);
    }

    private static void initialize(HttpServerResponse response) {
        if (response.bytesWritten() == 0) {
            MultiMap headers = response.headers();
            if (headers.get("content-type") == null) {
                headers.set("content-type", "text/event-stream");
            }
            response.setChunked(true);
        }
    }

    private static void onWriteDone(Subscription subscription, AsyncResult<Void> ar, RoutingContext rc) {
        if (ar.failed()) {
            rc.fail(ar.cause());
        } else {
            subscription.request(1);
        }
    }

    public static void write(Multi<Buffer> multi, RoutingContext rc) {
        HttpServerResponse response = rc.response();
        multi.subscribe().withSubscriber(new Subscriber<Buffer>() {
            Subscription upstream;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.upstream = subscription;
                this.upstream.request(1);
            }

            @Override
            public void onNext(Buffer item) {
                initialize(response);
                response.write(item, new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> ar) {
                        onWriteDone(upstream, ar, rc);
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                rc.fail(throwable);
            }

            @Override
            public void onComplete() {
                endOfStream(response);
            }
        });
    }

    public static void subscribeBuffer(Multi<Buffer> multi, RoutingContext rc) {
        HttpServerResponse response = rc.response();
        multi.subscribe().withSubscriber(new Subscriber<Buffer>() {
            Subscription upstream;
            final AtomicLong count = new AtomicLong();

            @Override
            public void onSubscribe(Subscription subscription) {
                this.upstream = subscription;
                this.upstream.request(1);
            }

            @Override
            public void onNext(Buffer item) {
                initialize(response);
                Buffer buffer = Buffer.buffer("data: ").appendBuffer(item).appendString("\n")
                        .appendString("id: " + count.getAndIncrement())
                        .appendString("\n\n");
                response.write(buffer, new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> ar) {
                        onWriteDone(upstream, ar, rc);
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                rc.fail(throwable);
            }

            @Override
            public void onComplete() {
                endOfStream(response);
            }
        });
    }

    public static void subscribeMutinyBuffer(Multi<io.vertx.mutiny.core.buffer.Buffer> multi, RoutingContext rc) {
        subscribeBuffer(multi.map(new Function<io.vertx.mutiny.core.buffer.Buffer, Buffer>() {
            @Override
            public Buffer apply(io.vertx.mutiny.core.buffer.Buffer b) {
                return b.getDelegate();
            }
        }), rc);
    }

    public static void subscribeObject(Multi<Object> multi, RoutingContext rc) {
        AtomicLong count = new AtomicLong();
        write(multi.map(new Function<Object, Buffer>() {
            @Override
            public Buffer apply(Object o) {
                if (o instanceof ReactiveRoutes.ServerSentEvent) {
                    ReactiveRoutes.ServerSentEvent<?> ev = (ReactiveRoutes.ServerSentEvent<?>) o;
                    long id = ev.id() != -1 ? ev.id() : count.getAndIncrement();
                    String e = ev.event() == null ? "" : "event: " + ev.event() + "\n";
                    return Buffer.buffer(e + "data: " + Json.encodeToBuffer(ev.data()) + "\nid: " + id + "\n\n");
                } else {
                    return Buffer.buffer("data: " + Json.encodeToBuffer(o) + "\nid: " + count.getAndIncrement() + "\n\n");
                }
            }
        }), rc);
    }

    private static void endOfStream(HttpServerResponse response) {
        if (response.bytesWritten() == 0) { // No item
            MultiMap headers = response.headers();
            if (headers.get("content-type") == null) {
                headers.set("content-type", "text/event-stream");
            }
        }
        response.end();
    }

    public static boolean isSSE(Multi<?> multi) {
        return multi instanceof SSEMulti;
    }
}
