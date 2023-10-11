package io.quarkus.redis.sessions.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.redis.deployment.client.spi.RedisClientBuildItem;
import io.quarkus.redis.deployment.client.spi.RequestedRedisClientBuildItem;
import io.quarkus.redis.runtime.spi.RedisConstants;
import io.quarkus.redis.sessions.runtime.RedisSessionsRecorder;
import io.quarkus.vertx.http.deployment.SessionStoreProviderBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.SessionsBuildTimeConfig;

public class RedisSessionsProcessor {
    @BuildStep
    public void redisClients(HttpBuildTimeConfig httpConfig,
            RedisSessionsBuildTimeConfig config,
            BuildProducer<RequestedRedisClientBuildItem> redisRequest) {
        if (httpConfig.sessions.mode == SessionsBuildTimeConfig.SessionsMode.REDIS) {
            String clientName = config.clientName.orElse(RedisConstants.DEFAULT_CLIENT_NAME);
            redisRequest.produce(new RequestedRedisClientBuildItem(clientName));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void redisSessions(HttpBuildTimeConfig httpConfig,
            RedisSessionsBuildTimeConfig config,
            List<RedisClientBuildItem> clients,
            BuildProducer<SessionStoreProviderBuildItem> provider,
            RedisSessionsRecorder recorder) {
        if (httpConfig.sessions.mode == SessionsBuildTimeConfig.SessionsMode.REDIS) {
            String clientName = config.clientName.orElse(RedisConstants.DEFAULT_CLIENT_NAME);
            for (RedisClientBuildItem redisClient : clients) {
                if (clientName.equals(redisClient.getName())) {
                    provider.produce(new SessionStoreProviderBuildItem(recorder.create(redisClient.getClient())));
                    return;
                }
            }
            throw new IllegalStateException("Unknown Redis client: " + clientName);
        }
    }
}
