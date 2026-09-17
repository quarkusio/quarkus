package io.quarkus.devui.runtime;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Tells whether a host name resolves to a loopback address only. The lookup blocks, so it runs on a worker thread,
 * and its result is cached for the lifetime of the resolver.
 */
final class LoopbackHostnameResolver {

    private final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();

    void isLoopback(String host, Vertx vertx, Handler<Boolean> handler) {
        Boolean cached = cache.get(host);
        if (cached != null) {
            handler.handle(cached);
            return;
        }
        vertx.executeBlocking(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return resolvesToLoopback(host);
            }
        }, false).onComplete(new Handler<AsyncResult<Boolean>>() {
            @Override
            public void handle(AsyncResult<Boolean> result) {
                boolean loopback = result.succeeded() && result.result();
                cache.put(host, loopback);
                handler.handle(loopback);
            }
        });
    }

    static boolean resolvesToLoopback(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (InetAddress address : addresses) {
                if (!address.isLoopbackAddress()) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
