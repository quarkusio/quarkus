package io.quarkus.redis.runtime.client;

import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS;
import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS_PROVIDER_NAME;
import static io.quarkus.redis.runtime.client.config.RedisConfig.getPropertyName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.arc.ActiveResult;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.codecs.Codec;
import io.quarkus.redis.datasource.codecs.Codecs;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Request;

@Recorder
public class RedisClientRecorder {
    // Split client and DS recorders
    private final RuntimeValue<RedisConfig> runtimeConfig;
    private static final Map<String, RedisClientAndApi> clients = new HashMap<>();
    private static final Map<String, ReactiveRedisDataSourceImpl> dataSources = new HashMap<>();
    private Vertx vertx;
    private ObservableRedisMetrics metrics;

    public RedisClientRecorder(RuntimeValue<RedisConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void initialize(RuntimeValue<io.vertx.core.Vertx> vertx, Set<String> names,
            Supplier<TlsConfigurationRegistry> tlsRegistrySupplier,
            Supplier<ProxyConfigurationRegistry> proxyRegistrySupplier) {
        Instance<ObservableRedisMetrics> instance = CDI.current().select(ObservableRedisMetrics.class);
        if (instance.isResolvable()) {
            this.metrics = instance.get();
        } else {
            this.metrics = null;
        }

        this.vertx = Vertx.newInstance(vertx.getValue());

        TlsConfigurationRegistry tlsRegistry = tlsRegistrySupplier.get();
        ProxyConfigurationRegistry proxyRegistry = proxyRegistrySupplier.get();

        _registerCodecs();

        _initialize(vertx.getValue(), names, tlsRegistry, proxyRegistry);
    }

    private static void _registerCodecs() {
        Instance<Codec> codecs = CDI.current().select(Codec.class);

        Codecs.register(codecs.stream());
    }

    public void _initialize(io.vertx.core.Vertx vertx, Set<String> names, TlsConfigurationRegistry tlsRegistry,
            ProxyConfigurationRegistry proxyRegistry) {
        for (String name : names) {
            if (checkActive(name).get().value()) {
                RedisClientConfig redisClientConfig = runtimeConfig.getValue().clients().get(name);
                clients.putIfAbsent(name, new RedisClientAndApi(name,
                        VertxRedisClientFactory.create(name, vertx, redisClientConfig, tlsRegistry, proxyRegistry), metrics));
            }
        }
    }

    public Supplier<Redis> getRedisClient(String name) {
        return new Supplier<Redis>() {
            @Override
            public Redis get() {
                return clients.get(name).redis;
            }
        };
    }

    public Supplier<io.vertx.redis.client.Redis> getBareRedisClient(String name) {
        return new Supplier<io.vertx.redis.client.Redis>() {
            @Override
            public io.vertx.redis.client.Redis get() {
                return clients.get(name).observable;
            }
        };
    }

    public Supplier<RedisAPI> getRedisAPI(String name) {
        return new Supplier<RedisAPI>() {
            @Override
            public RedisAPI get() {
                return clients.get(name).api;
            }
        };
    }

    public Supplier<io.vertx.redis.client.RedisAPI> getBareRedisAPI(String name) {
        return new Supplier<io.vertx.redis.client.RedisAPI>() {
            @Override
            public io.vertx.redis.client.RedisAPI get() {
                return clients.get(name).api.getDelegate();
            }
        };
    }

    public Supplier<ReactiveRedisDataSource> getReactiveDataSource(String name) {
        return new Supplier<ReactiveRedisDataSource>() {
            @Override
            public ReactiveRedisDataSource get() {
                return dataSources.computeIfAbsent(name, k -> {
                    RedisClientAndApi redisClientAndApi = clients.get(name);
                    Redis redis = redisClientAndApi.redis;
                    RedisAPI api = redisClientAndApi.api;
                    return new ReactiveRedisDataSourceImpl(vertx, redis, api);
                });
            }
        };
    }

    public Supplier<RedisDataSource> getBlockingDataSource(String name) {
        return new Supplier<RedisDataSource>() {
            @Override
            public RedisDataSource get() {
                Duration timeout = runtimeConfig.getValue().clients().get(name).timeout();
                return new BlockingRedisDataSourceImpl(
                        (ReactiveRedisDataSourceImpl) RedisClientRecorder.this.getReactiveDataSource(name).get(), timeout);
            }
        };
    }

    // Legacy client
    public Supplier<RedisClient> getLegacyRedisClient(String name) {
        return new Supplier<RedisClient>() {
            @Override
            public RedisClient get() {
                Duration timeout = runtimeConfig.getValue().clients().get(name).timeout();
                return new RedisClientImpl(
                        RedisClientRecorder.this.getRedisClient(name).get(),
                        RedisClientRecorder.this.getRedisAPI(name).get(),
                        timeout);
            }
        };
    }

    public Supplier<ReactiveRedisClient> getLegacyReactiveRedisClient(String name) {
        return new Supplier<ReactiveRedisClient>() {
            @Override
            public ReactiveRedisClient get() {
                return new ReactiveRedisClientImpl(RedisClientRecorder.this.getRedisClient(name).get(),
                        RedisClientRecorder.this.getRedisAPI(name).get());
            }
        };
    }

    public Supplier<ActiveResult> checkActive(final String name) {
        return new Supplier<>() {
            @Override
            public ActiveResult get() {
                RedisClientConfig redisClientConfig = runtimeConfig.getValue().clients().get(name);
                if (!redisClientConfig.active()) {
                    return ActiveResult.inactive(String.format(
                            """
                                    Redis Client '%s' was deactivated through configuration properties. \
                                    To activate the Redis Client, set configuration property '%s' to 'true' and configure the Redis Client '%s'. \
                                    Refer to https://quarkus.io/guides/redis-reference for guidance.
                                    """,
                            name, getPropertyName(name, "active"), name));
                }
                if (redisClientConfig.hosts().isEmpty() && redisClientConfig.hostsProviderName().isEmpty()) {
                    return ActiveResult.inactive(String.format(
                            """
                                    Redis Client '%s' was deactivated automatically because neither the hosts nor the hostsProviderName is set. \
                                    To activate the Redis Client, set the configuration property '%s' or '%s'. \
                                    Refer to https://quarkus.io/guides/redis-reference for guidance.
                                    """,
                            name, getPropertyName(name, HOSTS), getPropertyName(name, HOSTS_PROVIDER_NAME)));
                }
                return ActiveResult.active();
            }
        };
    }

    public void cleanup(ShutdownContext context) {
        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                for (RedisClientAndApi value : clients.values()) {
                    value.redis.close();
                }
                clients.clear();
                dataSources.clear();
            }
        });
    }

    public void preload(String name, List<String> loadScriptPaths, boolean redisFlushBeforeLoad, boolean redisLoadOnlyIfEmpty) {
        var tuple = clients.get(name);
        if (tuple == null) {
            throw new IllegalArgumentException("Unable import data into Redis - cannot find the Redis client " + name
                    + ", available clients are: " + clients.keySet());
        }

        if (redisFlushBeforeLoad) {
            tuple.redis.send(Request.cmd(Command.FLUSHALL)).await().indefinitely();
        } else if (redisLoadOnlyIfEmpty) {
            var list = tuple.redis.send(Request.cmd(Command.KEYS).arg("*")).await().indefinitely();
            if (list.size() != 0) {
                RedisDataLoader.LOGGER.debugf(
                        "Skipping the Redis data loading because the database is not empty: %d keys found", list.size());
                return;
            }
        }

        for (String path : loadScriptPaths) {
            RedisDataLoader.load(vertx, tuple.redis, path);
        }
    }

    private static class RedisClientAndApi {
        private final Redis redis;
        private final RedisAPI api;
        private final ObservableRedis observable;

        private RedisClientAndApi(String name, io.vertx.redis.client.Redis redis, ObservableRedisMetrics metrics) {
            this.observable = new ObservableRedis(redis, name, metrics);
            this.redis = Redis.newInstance(this.observable);
            this.api = RedisAPI.api(this.redis);
        }
    }
}
