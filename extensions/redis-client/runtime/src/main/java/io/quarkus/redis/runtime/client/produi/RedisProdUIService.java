package io.quarkus.redis.runtime.client.produi;

import static io.quarkus.redis.runtime.client.config.RedisConfig.DEFAULT_CLIENT_NAME;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.mutiny.TimeoutException;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

/**
 * Read-only Prod UI view of the configured Redis clients: for each client its
 * name, client type (standalone / cluster / sentinel / replication), configured
 * hosts, connection timeout and pool sizing, plus a live PING result (status,
 * response and latency).
 * <p>
 * There is no Dev UI data page to reuse (the Redis Dev UI is only a Dev Services
 * console link), so a bespoke read-only component + service is provided. The only
 * command it ever issues is {@code PING} - the same command the readiness health
 * check uses - so nothing is mutated. Host URIs are stripped of any embedded
 * {@code user:password@} credentials, and the configured password is never read,
 * so no secret is ever exposed.
 */
@ApplicationScoped
public class RedisProdUIService {

    @Inject
    RedisConfig config;

    @Inject
    @Any
    InjectableInstance<Redis> redis;

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the configured Redis clients and their live PING status (no secrets)")
    public List<RedisClientInfo> getClients() {
        List<RedisClientInfo> clients = new ArrayList<>();
        for (InstanceHandle<Redis> handle : redis.handles()) {
            if (!handle.getBean().isActive()) {
                continue;
            }
            String name = getClientName(handle.getBean());
            RedisClientConfig clientConfig = config.clients().get(name);
            clients.add(toClientInfo(name, clientConfig, handle.get()));
        }
        clients.sort(Comparator.comparing(RedisClientInfo::name));
        return clients;
    }

    private RedisClientInfo toClientInfo(String name, RedisClientConfig clientConfig, Redis client) {
        String displayName = DEFAULT_CLIENT_NAME.equals(name) ? "default" : name;

        String clientType = null;
        List<String> hosts = List.of();
        long timeoutMs = 0;
        int maxPoolSize = 0;
        int maxPoolWaiting = 0;
        Duration timeout = null;
        if (clientConfig != null) {
            clientType = clientConfig.clientType() == null ? null : clientConfig.clientType().name();
            hosts = sanitizeHosts(clientConfig);
            timeout = clientConfig.timeout();
            timeoutMs = timeout == null ? 0 : timeout.toMillis();
            maxPoolSize = clientConfig.maxPoolSize();
            maxPoolWaiting = clientConfig.maxPoolWaiting();
        }

        Ping ping = ping(client, timeout);

        return new RedisClientInfo(displayName, clientType, hosts, timeoutMs, maxPoolSize, maxPoolWaiting,
                ping.status, ping.response, ping.latencyMs, ping.error);
    }

    private Ping ping(Redis client, Duration timeout) {
        Duration atMost = timeout == null ? Duration.ofSeconds(10) : timeout;
        long start = System.nanoTime();
        try {
            Response response = client.send(Request.cmd(Command.PING)).await().atMost(atMost);
            long latency = (System.nanoTime() - start) / 1_000_000;
            return new Ping("UP", response == null ? null : response.toString(), latency, null);
        } catch (TimeoutException e) {
            return new Ping("DOWN", null, null, "Ping timed out");
        } catch (Exception e) {
            return new Ping("DOWN", null, null, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private List<String> sanitizeHosts(RedisClientConfig clientConfig) {
        if (clientConfig.hosts().isEmpty()) {
            return List.of();
        }
        List<String> hosts = new ArrayList<>();
        for (URI uri : clientConfig.hosts().get()) {
            hosts.add(sanitize(uri));
        }
        return hosts;
    }

    /**
     * Removes any credentials embedded in a Redis URI (the {@code user:password@}
     * userinfo component) so secrets are never exposed in the Prod UI.
     */
    private String sanitize(URI uri) {
        if (uri == null) {
            return null;
        }
        if (uri.getUserInfo() == null) {
            return uri.toString();
        }
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
        } catch (URISyntaxException e) {
            return uri.toString().replaceFirst("://[^/@]*@", "://");
        }
    }

    private static String getClientName(Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof RedisClientName redisClientName) {
                return redisClientName.value();
            }
        }
        return DEFAULT_CLIENT_NAME;
    }

    private record Ping(String status, String response, Long latencyMs, String error) {
    }

    public record RedisClientInfo(String name, String clientType, List<String> hosts, long timeoutMs, int maxPoolSize,
            int maxPoolWaiting, String pingStatus, String pingResponse, Long pingLatencyMs, String pingError) {
    }
}
