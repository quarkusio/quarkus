package io.quarkus.redis.runtime;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

@Recorder
public class RedisRecorder {

    public void configureTheClient(RedisConfig config, ShutdownContext shutdown) {
        QuarkusRedisCommandFactory redisCommandFactory = Arc.container().instance(QuarkusRedisCommandFactory.class).get();
        StatefulConnection<String, String> connection;

        Set<InetSocketAddress> addresses = config.hosts.get();
        if (!config.cluster.isPresent() || !config.cluster.get().enabled) {
            if (addresses.size() > 1) {
                throw new ConfigurationException("Multiple hosts supplied for non clustered configuration");
            }

            InetSocketAddress address = addresses.iterator().next();
            RedisURI redisURI = buildRedisURI(address, config);
            RedisClient redisClient = RedisClient.create(redisURI);
            connection = redisClient.connect();
        } else {
            List<RedisURI> nodes = new ArrayList<>();
            for (InetSocketAddress address : addresses) {
                RedisURI nodeURI = buildRedisURI(address, config);
                nodes.add(nodeURI);
            }
            RedisClusterClient redisClusterClient = RedisClusterClient.create(nodes);
            connection = new QuarkusRedisClusterConnection<>(redisClusterClient.connect());
        }

        redisCommandFactory.initialize(connection);
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                connection.close();
            }
        });
    }

    private RedisURI buildRedisURI(InetSocketAddress address, RedisConfig config) {
        RedisURI.Builder builder = RedisURI.Builder
                .redis(address.getHostName())
                .withPort(address.getPort())
                .withSsl(config.sslEnabled)
                .withDatabase(config.database);

        if (config.password.isPresent()) {
            builder.withPassword(config.password.get());
        }

        return builder.build();
    }
}
