package io.quarkus.vertx.core.runtime.produi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.Vertx;

/**
 * Read-only Prod UI view of the Vert.x instance: the event-loop and worker pool
 * sizing, execution-time guards and a few live flags of the running
 * {@link Vertx}. There is no Dev UI data page to reuse (the Vert.x Dev UI ships
 * only i18n bundles), so a bespoke read-only component + service is provided.
 * <p>
 * It reads only the always-present {@link Vertx} bean and {@link VertxConfiguration}
 * mapping and mutates nothing. None of the exposed values are sensitive (they are
 * thread-pool sizes and timing guards), so no secret is exposed.
 */
@ApplicationScoped
public class VertxProdUIService {

    @Inject
    Vertx vertx;

    @Inject
    VertxConfiguration config;

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Get a read-only view of the Vert.x event-loop and worker pool configuration and status")
    public VertxInfo getInfo() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        Integer configuredEventLoopPoolSize = config.eventLoopsPoolSize().isPresent()
                ? config.eventLoopsPoolSize().getAsInt()
                : null;
        int effectiveEventLoopPoolSize = configuredEventLoopPoolSize != null
                ? configuredEventLoopPoolSize
                : 2 * availableProcessors;
        Integer queueSize = config.queueSize().isPresent() ? config.queueSize().getAsInt() : null;

        return new VertxInfo(
                availableProcessors,
                effectiveEventLoopPoolSize,
                configuredEventLoopPoolSize,
                config.workerPoolSize(),
                config.internalBlockingPoolSize(),
                queueSize,
                config.maxEventLoopExecuteTime().toMillis(),
                config.maxWorkerExecuteTime().toMillis(),
                config.warningExceptionTime().toMillis(),
                config.blockedThreadCheckInterval().toMillis(),
                config.keepAliveTime().toSeconds(),
                config.caching(),
                config.classpathResolving(),
                config.prefill(),
                config.useAsyncDNS(),
                vertx.isClustered(),
                vertx.isNativeTransportEnabled());
    }

    public record VertxInfo(int availableProcessors, int eventLoopPoolSize, Integer eventLoopPoolConfigured,
            int workerPoolSize, int internalBlockingPoolSize, Integer queueSize, long maxEventLoopExecuteTimeMs,
            long maxWorkerExecuteTimeMs, long warningExceptionTimeMs, long blockedThreadCheckIntervalMs,
            long keepAliveTimeSeconds, boolean caching, boolean classpathResolving, boolean prefill,
            boolean useAsyncDNS, boolean clustered, boolean nativeTransportEnabled) {
    }
}
