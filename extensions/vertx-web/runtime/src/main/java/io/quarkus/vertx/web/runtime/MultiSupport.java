package io.quarkus.vertx.web.runtime;

import java.util.function.Consumer;
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
public class MultiSupport {

    private MultiSupport() {
        // Avoid direct instantiation.
    }

    public static void subscribeVoid(Multi<Void> multi, RoutingContext rc) {
        HttpServerResponse response = rc.response();
        multi.subscribe().with(
                new Consumer<Void>() {
                    @Override
                    public void accept(Void item) {
                        // do nothing
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable failure) {
                        rc.fail(failure);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        response.setStatusCode(204).end();
                    }
                });
    }

    public static void subscribeString(Multi<String> multi, RoutingContext rc) {
        HttpServerResponse response = rc.response();
        multi.subscribe().withSubscriber(new Subscriber<String>() {
            Subscription upstream;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.upstream = subscription;
                this.upstream.request(1);
            }

            @Override
            public void onNext(String item) {
                String toBeWritten;
                if (response.bytesWritten() == 0) {
                    response.setChunked(true);
                    MultiMap headers = response.headers();
                    if (headers.get("content-type") == null) {
                        headers.set("content-type", "application/json");
                    }
                    toBeWritten = "[\"" + item + "\"";
                } else {
                    toBeWritten = ",\"" + item + "\"";
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

    public static void subscribeBuffer(Multi<Buffer> multi, RoutingContext rc) {
        HttpServerResponse response = rc.response();
        multi.subscribe().withSubscriber(new Subscriber<Buffer>() {
            Subscription upstream;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.upstream = subscription;
                response.setChunked(true);
                this.upstream.request(1);
            }

            @Override
            public void onNext(Buffer item) {
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
                response.end();
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

    public static void subscribeRxBuffer(Multi<io.vertx.reactivex.core.buffer.Buffer> multi, RoutingContext rc) {
        subscribeBuffer(multi.map(new Function<io.vertx.reactivex.core.buffer.Buffer, Buffer>() {
            @Override
            public Buffer apply(io.vertx.reactivex.core.buffer.Buffer b) {
                return b.getDelegate();
            }
        }), rc);
    }

    public static void subscribeObject(Multi<Object> multi, RoutingContext rc) {
        HttpServerResponse response = rc.response();
        multi.subscribe().withSubscriber(new Subscriber<Object>() {
            Subscription upstream;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.upstream = subscription;
                this.upstream.request(1);
            }

            @Override
            public void onNext(Object item) {
                String toBeWritten;
                if (response.bytesWritten() == 0) {
                    response.setChunked(true);
                    MultiMap headers = response.headers();
                    if (headers.get("content-type") == null) {
                        headers.set("content-type", "application/json");
                    }
                    toBeWritten = "[" + Json.encodeToBuffer(item);
                } else {
                    toBeWritten = "," + Json.encodeToBuffer(item);
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

}
