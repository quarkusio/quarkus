package io.quarkus.vertx.web.runtime;

import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

@SuppressWarnings("ReactiveStreamsSubscriberImplementation")
public class MultiJsonArraySupport {

    private MultiJsonArraySupport() {
        // Avoid direct instantiation.
    }

    public static void subscribeVoid(Multi<Void> multi, RoutingContext rc) {
        subscribeString(multi.onItem().castTo(String.class), rc);
    }

    public static void subscribeString(Multi<String> multi, RoutingContext rc) {
        write(multi.map(new Function<String, Buffer>() {
            @Override
            public Buffer apply(String s) {
                return Buffer.buffer("\"" + s + "\"");
            }
        }), rc);
    }

    private static void write(Multi<Buffer> multi, RoutingContext rc) {
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
                Buffer toBeWritten;
                if (response.bytesWritten() == 0) {
                    response.setChunked(true);
                    MultiMap headers = response.headers();
                    if (headers.get("content-type") == null) {
                        headers.set("content-type", "application/json");
                    }
                    toBeWritten = Buffer.buffer("[").appendBuffer(item);
                } else {
                    toBeWritten = Buffer.buffer(",").appendBuffer(item);
                }
                response.write(toBeWritten, new Handler<AsyncResult<Void>>() {
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
                completeJsonArray(response);
            }
        });
    }

    private static void onWriteDone(Subscription subscription, AsyncResult<Void> ar, RoutingContext rc) {
        if (ar.failed()) {
            rc.fail(ar.cause());
        } else {
            subscription.request(1);
        }
    }

    public static void subscribeObject(Multi<Object> multi, RoutingContext rc) {
        write(multi.map(new Function<Object, Buffer>() {
            @Override
            public Buffer apply(Object item) {
                return Json.encodeToBuffer(item);
            }
        }), rc);
    }

    public static void fail(RoutingContext rc) {
        rc.fail(new Exception("Unsupported type"));
    }

    private static void completeJsonArray(HttpServerResponse response) {
        if (response.bytesWritten() == 0) { // No item
            MultiMap headers = response.headers();
            if (headers.get("content-type") == null) {
                headers.set("content-type", "application/json");
            }
            response.end("[]");
        } else {
            response.end("]");
        }
    }

    public static boolean isJsonArray(Multi<?> multi) {
        return multi instanceof JsonArrayMulti;
    }

}
