package io.quarkus.micrometer.runtime.binder.netty;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.netty4.NettyEventExecutorMetrics;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;

@Singleton
public class VertxNettyEventExecutorMetricsProvider {

    @Produces
    @Singleton
    public MeterBinder vertxEventLoopGroupMetrics(Vertx vertx) {
        VertxInternal vi = (VertxInternal) vertx;
        return new NettyEventExecutorMetrics(vi.eventLoopGroup(), Tags.of("executor", "eventLoop"));
    }

    @Produces
    @Singleton
    public MeterBinder vertxAcceptorEventLoopGroupMetrics(Vertx vertx) {
        VertxInternal vi = (VertxInternal) vertx;
        return new NettyEventExecutorMetrics(vi.acceptorEventLoopGroup(), Tags.of("executor", "acceptor"));
    }

}
