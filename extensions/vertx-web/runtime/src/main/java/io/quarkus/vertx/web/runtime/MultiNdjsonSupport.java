package io.quarkus.vertx.web.runtime;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

@SuppressWarnings("ReactiveStreamsSubscriberImplementation")
public class MultiNdjsonSupport {

    private MultiNdjsonSupport() {
        // Avoid direct instantiation.
    }

    private static void initialize(HttpServerResponse response, RoutingContext rc) {
        if (response.bytesWritten() == 0) {
            MultiMap headers = response.headers();
            if (headers.get(HttpHeaders.CONTENT_TYPE) == null) {
                if (rc.getAcceptableContentType() == null) {
                    headers.set(HttpHeaders.CONTENT_TYPE, "application/x-ndjson");
                } else {
                    headers.set(HttpHeaders.CONTENT_TYPE, rc.getAcceptableContentType());
                }
            }
            response.setChunked(true);
        }
    }

    public static void subscribeString(Multi<String> multi, RoutingContext rc) {
        write(multi.map(s -> Buffer.buffer("\"" + s + "\"\n")), rc);
    }

    public static void subscribeObject(Multi<Object> multi, RoutingContext rc) {
        write(multi.map(o -> Buffer.buffer(Json.encode(o) + "\n")), rc);
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
                initialize(response, rc);
                response.write(item, ar -> onWriteDone(upstream, ar, rc));
            }

            @Override
            public void onError(Throwable throwable) {
                rc.fail(throwable);
            }

            @Override
            public void onComplete() {
                endOfStream(response, rc);
            }
        });
    }

    private static void endOfStream(HttpServerResponse response, RoutingContext rc) {
        if (response.bytesWritten() == 0) { // No item
            MultiMap headers = response.headers();
            if (headers.get(HttpHeaders.CONTENT_TYPE) == null) {
                if (rc.getAcceptableContentType() == null) {
                    headers.set(HttpHeaders.CONTENT_TYPE, "application/x-ndjson");
                } else {
                    headers.set(HttpHeaders.CONTENT_TYPE, rc.getAcceptableContentType());
                }
            }
        }
        response.end();
    }

    public static boolean isNdjson(Multi<?> multi) {
        return multi instanceof NdjsonMulti;
    }
}
