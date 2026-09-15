package io.quarkus.mongodb.runtime.produi;

import static io.quarkus.mongodb.runtime.MongoClientBeanUtil.mongoClientName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.bson.Document;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the configured MongoDB clients: for each client
 * (blocking and reactive) its name, hosts, database, application name, replica
 * set, pool sizing and a live {@code ping} result (status and latency).
 * <p>
 * There is no Dev UI data page to reuse (the MongoDB Dev UI is only a Dev
 * Services console link), so a bespoke read-only component + service is provided.
 * The only command it ever issues is {@code ping} - the same command the
 * readiness health check uses - so nothing is mutated. Connection strings are
 * stripped of any embedded {@code user:password@} credentials, and the
 * configured credentials are never read, so no secret is exposed.
 */
@ApplicationScoped
public class MongoProdUIService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Document PING = new Document("ping", 1);

    @Inject
    MongoConfig mongoConfig;

    @Inject
    @Any
    InjectableInstance<MongoClient> mongoClients;

    @Inject
    @Any
    InjectableInstance<ReactiveMongoClient> reactiveMongoClients;

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the configured MongoDB clients and their live ping status (no secrets)")
    public List<MongoClientInfo> getClients() {
        List<MongoClientInfo> clients = new ArrayList<>();

        for (InstanceHandle<MongoClient> handle : mongoClients.handles()) {
            if (!handle.getBean().isActive()) {
                continue;
            }
            String clientName = mongoClientName(handle.getBean());
            MongoClientConfig clientConfig = mongoConfig.clients().get(clientName);
            clients.add(toClientInfo(MongoConfig.nameOrDefault(clientName), "blocking", clientConfig,
                    pingBlocking(handle.get(), clientConfig)));
        }

        for (InstanceHandle<ReactiveMongoClient> handle : reactiveMongoClients.handles()) {
            if (!handle.getBean().isActive()) {
                continue;
            }
            String clientName = mongoClientName(handle.getBean());
            MongoClientConfig clientConfig = mongoConfig.clients().get(clientName);
            clients.add(toClientInfo(MongoConfig.reactiveNameOrDefault(clientName), "reactive", clientConfig,
                    pingReactive(handle.get(), clientConfig)));
        }

        clients.sort(Comparator.comparing(MongoClientInfo::name));
        return clients;
    }

    private MongoClientInfo toClientInfo(String name, String type, MongoClientConfig clientConfig, Ping ping) {
        List<String> hosts = List.of();
        String connectionString = null;
        String database = null;
        String applicationName = null;
        String replicaSetName = null;
        Integer maxPoolSize = null;
        Integer minPoolSize = null;
        String healthDatabase = null;
        if (clientConfig != null) {
            hosts = clientConfig.hosts().orElse(List.of());
            connectionString = clientConfig.connectionString().map(this::sanitize).orElse(null);
            database = clientConfig.database().orElse(null);
            applicationName = clientConfig.applicationName().orElse(null);
            replicaSetName = clientConfig.replicaSetName().orElse(null);
            maxPoolSize = toInteger(clientConfig.maxPoolSize());
            minPoolSize = toInteger(clientConfig.minPoolSize());
            healthDatabase = clientConfig.healthDatabase();
        }
        return new MongoClientInfo(name, type, hosts, connectionString, database, applicationName, replicaSetName,
                maxPoolSize, minPoolSize, healthDatabase, ping.status, ping.latencyMs, ping.error);
    }

    private Ping pingBlocking(MongoClient client, MongoClientConfig config) {
        long start = System.nanoTime();
        try {
            client.getDatabase(healthDatabase(config)).runCommand(PING);
            return new Ping("UP", latencyMs(start), null);
        } catch (Exception e) {
            return new Ping("DOWN", null, message(e));
        }
    }

    private Ping pingReactive(ReactiveMongoClient client, MongoClientConfig config) {
        Duration timeout = config == null ? DEFAULT_TIMEOUT : config.readTimeout().orElse(DEFAULT_TIMEOUT);
        long start = System.nanoTime();
        try {
            client.getDatabase(healthDatabase(config)).runCommand(PING).await().atMost(timeout);
            return new Ping("UP", latencyMs(start), null);
        } catch (Exception e) {
            return new Ping("DOWN", null, message(e));
        }
    }

    private String healthDatabase(MongoClientConfig config) {
        return config == null ? "admin" : config.healthDatabase();
    }

    private Long latencyMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String message(Exception e) {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    private Integer toInteger(OptionalInt value) {
        return value != null && value.isPresent() ? value.getAsInt() : null;
    }

    /**
     * Removes any credentials embedded in a MongoDB connection string (the
     * {@code user:password@} userinfo component) so secrets are never exposed.
     */
    private String sanitize(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            return connectionString;
        }
        return connectionString.replaceFirst("://[^/@]*@", "://");
    }

    private record Ping(String status, Long latencyMs, String error) {
    }

    public record MongoClientInfo(String name, String type, List<String> hosts, String connectionString, String database,
            String applicationName, String replicaSetName, Integer maxPoolSize, Integer minPoolSize,
            String healthDatabase, String pingStatus, Long pingLatencyMs, String pingError) {
    }
}
