package io.quarkus.redis.datasource;

import java.util.List;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

/**
 * A {@link RedisConnection} forwarding everything to a delegate, so tests can alter a single operation.
 */
class DelegatingRedisConnection implements RedisConnection {

    final RedisConnection delegate;

    DelegatingRedisConnection(RedisConnection delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<Response> send(Request command) {
        return delegate.send(command);
    }

    @Override
    public Future<List<Response>> batch(List<Request> commands) {
        return delegate.batch(commands);
    }

    @Override
    public Future<Void> close() {
        return delegate.close();
    }

    @Override
    public boolean pendingQueueFull() {
        return delegate.pendingQueueFull();
    }

    @Override
    public RedisConnection exceptionHandler(Handler<Throwable> handler) {
        delegate.exceptionHandler(handler);
        return this;
    }

    @Override
    public RedisConnection handler(Handler<Response> handler) {
        delegate.handler(handler);
        return this;
    }

    @Override
    public RedisConnection pause() {
        delegate.pause();
        return this;
    }

    @Override
    public RedisConnection resume() {
        delegate.resume();
        return this;
    }

    @Override
    public RedisConnection fetch(long amount) {
        delegate.fetch(amount);
        return this;
    }

    @Override
    public RedisConnection endHandler(Handler<Void> endHandler) {
        delegate.endHandler(endHandler);
        return this;
    }
}
