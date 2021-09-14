package io.quarkus.redis.client.runtime;

import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.redis.client.runtime.RedisConfig.RedisConfiguration;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.net.NetClientOptions;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;

public class RedisClientUtil {
    public static final String DEFAULT_CLIENT = "<default>";

    public static RedisOptions buildOptions(RedisConfiguration redisConfig) {
        RedisOptions options = new RedisOptions();
        options.setType(redisConfig.clientType);
        Set<URI> hosts = Collections.emptySet();

        if (redisConfig.hosts.isPresent()) {
            hosts = redisConfig.hosts.get();
        } else if (redisConfig.hostsProviderName.isPresent()) {
            RedisHostsProvider hostsProvider = findProvider(redisConfig.hostsProviderName.get());
            hosts = hostsProvider.getHosts();
        }

        if (RedisClientType.STANDALONE == redisConfig.clientType) {
            if (hosts.size() > 1) {
                throw new ConfigurationException("Multiple hosts supplied for non clustered configuration");
            }
        }

        for (URI host : hosts) {
            options.addConnectionString(host.toString());
        }

        options.setMaxNestedArrays(redisConfig.maxNestedArrays);
        options.setMaxWaitingHandlers(redisConfig.maxWaitingHandlers);
        options.setMaxPoolSize(redisConfig.maxPoolSize);
        options.setMaxPoolWaiting(redisConfig.maxPoolWaiting);
        options.setPoolRecycleTimeout(Math.toIntExact(redisConfig.poolRecycleTimeout.toMillis()));

        if (redisConfig.poolCleanerInterval.isPresent()) {
            options.setPoolCleanerInterval(Math.toIntExact(redisConfig.poolCleanerInterval.get().toMillis()));
        }

        if (redisConfig.role.isPresent()) {
            options.setRole(redisConfig.role.get());
        }

        if (redisConfig.masterName.isPresent()) {
            options.setMasterName(redisConfig.masterName.get());
        }

        if (redisConfig.replicas.isPresent()) {
            options.setUseReplicas(redisConfig.replicas.get());
        }

        if (redisConfig.password.isPresent()) {
            options.setPassword(redisConfig.password.get());
        }

        options.setNetClientOptions(toNetClientOptions(redisConfig));

        return options;
    }

    private static NetClientOptions toNetClientOptions(RedisConfiguration redisConfig) {
        NetClientOptions netClientOptions = new NetClientOptions()
                .setTcpKeepAlive(redisConfig.tcpKeepAlive)
                .setTcpNoDelay(redisConfig.tcpNoDelay);

        SslConfig sslConfig = redisConfig.ssl;

        netClientOptions
                .setSsl(sslConfig.enabled)
                .setTrustAll(sslConfig.trustAll);

        configurePemTrustOptions(netClientOptions, sslConfig.trustCertificatePem);
        configureJksTrustOptions(netClientOptions, sslConfig.trustCertificateJks);
        configurePfxTrustOptions(netClientOptions, sslConfig.trustCertificatePfx);

        configurePemKeyCertOptions(netClientOptions, sslConfig.keyCertificatePem);
        configureJksKeyCertOptions(netClientOptions, sslConfig.keyCertificateJks);
        configurePfxKeyCertOptions(netClientOptions, sslConfig.keyCertificatePfx);

        netClientOptions.setReconnectAttempts(redisConfig.reconnectAttempts);
        netClientOptions.setReconnectInterval(redisConfig.reconnectInterval.toMillis());

        if (redisConfig.idleTimeout.isPresent()) {
            netClientOptions.setIdleTimeout(redisConfig.idleTimeout.get());
        }

        if (sslConfig.hostnameVerificationAlgorithm.isPresent()) {
            netClientOptions.setHostnameVerificationAlgorithm(
                    sslConfig.hostnameVerificationAlgorithm.get());
        }

        return netClientOptions;
    }

    public static boolean isDefault(String clientName) {
        return DEFAULT_CLIENT.equals(clientName);
    }

    public static RedisConfiguration getConfiguration(RedisConfig config, String name) {
        if (isDefault(name)) {
            return config.defaultClient;
        }

        RedisConfiguration redisConfiguration = config.additionalRedisClients.get(name);
        if (redisConfiguration != null) {
            return redisConfiguration;
        }

        throw new IllegalArgumentException(String.format("Configuration for %s redis client does not exists", name));
    }

    public static RedisHostsProvider findProvider(String name) {
        ArcContainer container = Arc.container();
        RedisHostsProvider hostsProvider = name != null
                ? (RedisHostsProvider) container.instance(name).get()
                : container.instance(RedisHostsProvider.class).get();

        if (hostsProvider == null) {
            throw new RuntimeException("unable to find redis host provider named: " + (name == null ? "default" : name));
        }

        return hostsProvider;
    }
}
