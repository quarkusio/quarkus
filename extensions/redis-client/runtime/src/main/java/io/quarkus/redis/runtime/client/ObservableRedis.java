package io.quarkus.redis.runtime.client;

import java.util.List;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

/**
 * An implementation of the {@link Redis} interface that tracks the duration of each operation for observability
 * purpose.
 */
public class ObservableRedis implements Redis {

    private final Redis redis;
    private final String name;
    private final ObservableRedisMetrics reporter;

    public ObservableRedis(Redis redis, String name, ObservableRedisMetrics reporter) {
        this.redis = redis;
        this.name = name;
        this.reporter = reporter == null ? ObservableRedisMetrics.NOOP : reporter;
    }

    public String name() {
        return name;
    }

    private void report(long time, boolean succeeded) {
        reporter.report(name, time, succeeded);
    }

    @Override
    public Redis connect(Handler<AsyncResult<RedisConnection>> handler) {
        this.redis.connect(ar -> {
            if (ar.failed()) {
                handler.handle(Future.failedFuture(ar.cause()));
            } else {
                handler.handle(Future.succeededFuture(new ObservableRedisConnection(ar.result())));
            }
        });
        return this;
    }

    @Override
    public Redis send(Request command, Handler<AsyncResult<@Nullable Response>> onSend) {
        long begin = System.nanoTime();
        redis.send(command, ar -> {
            report(System.nanoTime() - begin, ar.succeeded());
            onSend.handle(ar);
        });
        return this;
    }

    @Override
    public Redis batch(List<Request> commands, Handler<AsyncResult<List<@Nullable Response>>> onSend) {
        long begin = System.nanoTime();
        redis.batch(commands, ar -> {
            report(System.nanoTime() - begin, ar.succeeded());
            onSend.handle(ar);
        });
        return this;
    }

    @Override
    public Future<RedisConnection> connect() {
        return redis.connect().map(ObservableRedisConnection::new);
    }

    @Override
    public void close() {
        redis.close();
    }

    @Override
    public Future<Response> send(Request command) {
        long begin = System.nanoTime();
        return redis.send(command).onComplete(x -> report(System.nanoTime() - begin, x.succeeded()));
    }

    @Override
    public Future<List<Response>> batch(List<Request> commands) {
        long begin = System.nanoTime();
        return redis.batch(commands).onComplete(x -> report(System.nanoTime() - begin, x.succeeded()));
    }

    private class ObservableRedisConnection implements RedisConnection {

        private final RedisConnection delegate;

        private ObservableRedisConnection(RedisConnection delegate) {
            this.delegate = delegate;
        }

        @Override
        public RedisConnection exceptionHandler(Handler<Throwable> handler) {
            delegate.exceptionHandler(handler);
            return this;
        }

        @Override
        public RedisConnection handler(@Nullable Handler<Response> handler) {
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
        public RedisConnection endHandler(@Nullable Handler<Void> endHandler) {
            delegate.endHandler(endHandler);
            return this;
        }

        @Override
        public Future<Response> send(Request command) {
            long begin = System.nanoTime();
            return delegate.send(command).onComplete(ar -> {
                long end = System.nanoTime();
                report(end - begin, ar.succeeded());
            });
        }

        @Override
        public Future<List<Response>> batch(List<Request> commands) {
            long begin = System.nanoTime();
            return delegate.batch(commands).onComplete(ar -> {
                long end = System.nanoTime();
                report(end - begin, ar.succeeded());
            });
        }

        @Override
        public Future<Void> close() {
            return delegate.close();
        }

        @Override
        public boolean pendingQueueFull() {
            return delegate.pendingQueueFull();
        }
    }
}
