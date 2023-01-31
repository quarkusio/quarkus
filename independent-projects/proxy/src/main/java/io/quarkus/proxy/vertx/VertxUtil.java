package io.quarkus.proxy.vertx;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.impl.ContextInternal;

public class VertxUtil {

    public static <T> Promise<T> toPromise(Context context, io.netty.util.concurrent.Future<T> nettyFuture) {
        var promise = ((ContextInternal) context).<T>promise();
        nettyFuture.addListener(fut -> {
            if (fut.isSuccess()) {
                promise.complete();
            } else {
                promise.fail(fut.cause());
            }
        });
        return promise;
    }
}
