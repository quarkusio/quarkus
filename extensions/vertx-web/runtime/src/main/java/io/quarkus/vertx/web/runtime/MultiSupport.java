package io.quarkus.vertx.web.runtime;

import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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
        subscribeBuffer(multi.map(s -> Buffer.buffer(s)), rc);
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
                if (response.bytesWritten() == 0) {
                    response.setStatusCode(204);
                }
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

    public static void subscribeObject(Multi<Object> multi, RoutingContext rc) {
        subscribeBuffer(multi.map(new Function<Object, Buffer>() {
            @Override
            public Buffer apply(Object o) {
                return Json.encodeToBuffer(o);
            }
        }), rc);
    }

}
