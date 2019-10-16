package io.quarkus.redis.runtime.graal;

import java.io.Closeable;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionEvents;
import io.lettuce.core.RedisURI;
import io.lettuce.core.event.DefaultEventBus;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.metrics.DefaultCommandLatencyCollector;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DefaultEventLoopGroupProvider;
import io.lettuce.core.resource.Delay;
import io.lettuce.core.resource.DnsResolver;
import io.lettuce.core.resource.DnsResolvers;
import io.lettuce.core.resource.EventLoopGroupProvider;
import io.lettuce.core.resource.NettyCustomizer;
import io.lettuce.core.resource.SocketAddressResolver;
import io.lettuce.core.tracing.Tracing;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.ConcurrentSet;
import io.quarkus.redis.runtime.Metrics;
import reactor.core.scheduler.Schedulers;

/**
 * Substitutions that are necessary to construct a default {@link DefaultClientResources} with collection of latency metrics
 * being disabled.
 * This is done only when the "org.LatencyUtils.PauseDetector" and "org.HdrHistogram.Histogram" classes are missing.
 */

class DisableMetrics implements java.util.function.BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return !Metrics.enabled();
    }
}

@TargetClass(value = AbstractRedisClient.class, onlyWith = DisableMetrics.class)
final class AbstractRedisClientSubstitute {
    @Alias
    protected Map<Class<? extends EventLoopGroup>, EventLoopGroup> eventLoopGroups = new ConcurrentHashMap<>(2);
    @Alias
    protected ConnectionEvents connectionEvents = new ConnectionEvents();

    @Alias
    @SuppressWarnings("deprecation")
    protected Set<Closeable> closeableResources = new ConcurrentSet<>();

    @Alias
    protected ClientOptions clientOptions = ClientOptions.builder().build();

    @Alias
    protected EventExecutorGroup genericWorkerPool;

    @Alias
    protected HashedWheelTimer timer;

    @Alias
    protected ChannelGroup channels;

    @Alias
    protected ClientResources clientResources;
    @Alias
    protected Duration timeout;

    @Alias
    private AtomicBoolean shutdown;

    @Substitute
    protected AbstractRedisClientSubstitute(ClientResources clientResources) {
        this.shutdown = new AtomicBoolean();
        this.timeout = RedisURI.DEFAULT_TIMEOUT_DURATION;
        this.clientResources = DefaultClientResources.builder().build();
        this.genericWorkerPool = this.clientResources.eventExecutorGroup();
        this.channels = new DefaultChannelGroup(this.genericWorkerPool.next());
        this.timer = (HashedWheelTimer) this.clientResources.timer();
    }
}

@Delete
@TargetClass(value = DefaultCommandLatencyCollector.class, onlyWith = DisableMetrics.class)
final class DeleteDefaultMetricsCollector {
}

@TargetClass(value = DefaultClientResources.class, onlyWith = DisableMetrics.class)
final class DisableMetricsDefaultClentResources {
    @Alias
    private EventLoopGroupProvider eventLoopGroupProvider;
    @Alias
    private EventExecutorGroup eventExecutorGroup;
    @Alias
    private Timer timer;
    @Alias
    private EventBus eventBus;
    @Alias
    private CommandLatencyCollector commandLatencyCollector;
    @Alias
    private DnsResolver dnsResolver;
    @Alias
    private SocketAddressResolver socketAddressResolver;
    @Alias
    private Supplier<Delay> reconnectDelay;
    @Alias
    private NettyCustomizer nettyCustomizer;
    @Alias
    private Tracing tracing;

    @Substitute
    protected DisableMetricsDefaultClentResources(DefaultClientResources.Builder builder) {
        this.timer = new HashedWheelTimer(new DefaultThreadFactory("lettuce-timer"));
        this.commandLatencyCollector = CommandLatencyCollector.disabled();
        this.tracing = Tracing.disabled();
        this.dnsResolver = DnsResolvers.UNRESOLVED;
        this.eventLoopGroupProvider = new DefaultEventLoopGroupProvider(1);
        this.eventExecutorGroup = DefaultEventLoopGroupProvider.createEventLoopGroup(DefaultEventExecutorGroup.class, 1);
        this.eventBus = new DefaultEventBus(Schedulers.fromExecutor(this.eventExecutorGroup));
        this.nettyCustomizer = new NettyCustomizer() {
            @Override
            public void afterBootstrapInitialized(Bootstrap bootstrap) {

            }

            @Override
            public void afterChannelInitialized(Channel channel) {

            }
        };

        this.reconnectDelay = Delay::exponential;
        this.socketAddressResolver = SocketAddressResolver.create(this.dnsResolver);
    }
}
