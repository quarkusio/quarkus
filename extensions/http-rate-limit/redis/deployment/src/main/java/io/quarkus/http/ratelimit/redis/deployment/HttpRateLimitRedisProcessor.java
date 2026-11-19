package io.quarkus.http.ratelimit.redis.deployment;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.redis.runtime.internal.RedisRateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.deployment.client.RequestedRedisClientBuildItem;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.smallrye.common.annotation.Identifier;

public class HttpRateLimitRedisProcessor {

    private static final String REDIS = "redis";

    /**
     * Registers the built-in {@code redis} store, only when a rule uses it, as a synthetic
     * {@code @Identifier("redis")} bean backed by a service. The service builds the store from the
     * {@link ReactiveRedisDataSource} service of the client named by {@code quarkus.http.rate-limit.redis.client-name},
     * or of the default client, and {@link RequestedRedisClientBuildItem} makes the Redis client extension create that
     * client, and no other.
     * <p>
     * The store does not inject the data source: an {@code Instance<ReactiveRedisDataSource>} lookup would make the Redis
     * client extension create every configured client.
     */
    @BuildStep
    void redisStore(HttpRateLimitRedisBuildTimeConfig config, HttpRateLimitBuildTimeConfig buildConfig,
            ServiceRegistrar serviceRegistrar, BuildProducer<RequestedRedisClientBuildItem> requestedClients,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        if (!usesRedis(buildConfig)) {
            return;
        }
        String client = config.redisClientName().orElse(RedisConfig.DEFAULT_CLIENT_NAME);
        requestedClients.produce(new RequestedRedisClientBuildItem(client));

        // the data source service is optional: it is absent when the client is not active
        serviceRegistrar.forService(RedisRateLimitStore.class)
                .atPhase(Phase.DATA)
                .request(ReactiveRedisDataSource.class, client)
                .require(HttpRateLimitRuntimeConfig.class)
                .optional()
                .onStart((ctx, dataSource, runtimeConfig) -> dataSource
                        .map(ds -> new RedisRateLimitStore(ds, runtimeConfig)));

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(RedisRateLimitStore.class)
                .types(RateLimitStore.class)
                .addQualifier().annotation(Identifier.class).addValue("value", REDIS).done()
                .scope(Singleton.class)
                .setRuntimeInit()
                .serviceValue(RedisRateLimitStore.class)
                .done());
    }

    private static boolean usesRedis(HttpRateLimitBuildTimeConfig config) {
        for (HttpRateLimitBuildTimeConfig.Rule rule : config.rules().values()) {
            if (REDIS.equals(RateLimitRuleConfig.store(config, rule))) {
                return true;
            }
        }
        return false;
    }

    @BuildStep
    NativeImageResourceBuildItem luaScripts() {
        return new NativeImageResourceBuildItem("META-INF/http-rate-limit/try-consume.lua");
    }
}
