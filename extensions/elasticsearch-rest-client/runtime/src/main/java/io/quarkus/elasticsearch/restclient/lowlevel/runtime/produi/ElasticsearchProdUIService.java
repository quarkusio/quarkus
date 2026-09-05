package io.quarkus.elasticsearch.restclient.lowlevel.runtime.produi;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.hc.core5.http.io.entity.EntityUtils;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.ElasticsearchConfig;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.vertx.core.json.JsonObject;

/**
 * Read-only Prod UI view of the Elasticsearch REST client: the configured hosts,
 * protocol, connection pool sizing and timeouts, plus the live cluster health
 * (status, node and shard counts) obtained from {@code GET /_cluster/health}.
 * <p>
 * There is no Dev UI data page to reuse (the Elasticsearch Dev UI is only a Dev
 * Services console link), so a bespoke read-only component + service is provided.
 * The only request it issues is the read-only {@code _cluster/health} query - the
 * same one the readiness health check uses - so nothing is mutated. The
 * configured username, password and API key are never read, so no secret is
 * exposed.
 */
@ApplicationScoped
public class ElasticsearchProdUIService {

    @Inject
    Rest5Client restClient;

    @Inject
    ElasticsearchConfig config;

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the Elasticsearch client configuration and live cluster health (no secrets)")
    public ElasticsearchInfo getInfo() {
        List<String> hosts = new ArrayList<>();
        for (InetSocketAddress host : config.hosts()) {
            hosts.add(host.getHostString() + ":" + host.getPort());
        }

        ClusterHealth clusterHealth = null;
        String error = null;
        Long latencyMs = null;
        long start = System.nanoTime();
        try {
            Request request = new Request("GET", "/_cluster/health");
            Response response = restClient.performRequest(request);
            String body = EntityUtils.toString(response.getEntity());
            latencyMs = (System.nanoTime() - start) / 1_000_000;
            JsonObject json = new JsonObject(body);
            clusterHealth = new ClusterHealth(
                    json.getString("cluster_name"),
                    json.getString("status"),
                    json.getInteger("number_of_nodes", 0),
                    json.getInteger("number_of_data_nodes", 0),
                    json.getInteger("active_primary_shards", 0),
                    json.getInteger("active_shards", 0),
                    json.getInteger("relocating_shards", 0),
                    json.getInteger("initializing_shards", 0),
                    json.getInteger("unassigned_shards", 0));
        } catch (Exception e) {
            error = e.getMessage() == null ? e.toString() : e.getMessage();
        }

        return new ElasticsearchInfo(
                hosts,
                config.protocol(),
                config.maxConnections(),
                config.maxConnectionsPerRoute(),
                config.connectionTimeout().toMillis(),
                config.socketTimeout().toMillis(),
                clusterHealth,
                error,
                latencyMs);
    }

    public record ElasticsearchInfo(List<String> hosts, String protocol, int maxConnections, int maxConnectionsPerRoute,
            long connectionTimeoutMs, long socketTimeoutMs, ClusterHealth clusterHealth, String error, Long latencyMs) {
    }

    public record ClusterHealth(String clusterName, String status, int numberOfNodes, int numberOfDataNodes,
            int activePrimaryShards, int activeShards, int relocatingShards, int initializingShards,
            int unassignedShards) {
    }
}
