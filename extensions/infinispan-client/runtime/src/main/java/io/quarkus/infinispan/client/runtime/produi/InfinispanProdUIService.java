package io.quarkus.infinispan.client.runtime.produi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.ServerStatistics;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.infinispan.client.InfinispanClientName;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the configured Infinispan clients. For each client
 * (default + named) it exposes the cluster members (server host:port), the
 * caches and their server-side hit/miss statistics, derived from the
 * always-present {@link RemoteCacheManager} beans. It deliberately reads only
 * server addresses and statistics - never the security configuration
 * (authentication / SSL keystore passwords) - so no secrets are exposed, and it
 * performs no cache mutation (no put/remove/clear). The Dev UI only links to the
 * dev-services management console, which is not available in production, so this
 * is a bespoke read-only view.
 */
@ApplicationScoped
public class InfinispanProdUIService {

    private static final Logger LOG = Logger.getLogger(InfinispanProdUIService.class);

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only overview of the Infinispan clients: cluster members, caches and hit/miss statistics")
    public List<ClientInfo> getClients() {
        List<ClientInfo> result = new ArrayList<>();
        Iterable<InstanceHandle<RemoteCacheManager>> handles = Arc.container()
                .select(RemoteCacheManager.class, Any.Literal.INSTANCE)
                .handles();
        for (InstanceHandle<RemoteCacheManager> handle : handles) {
            result.add(describe(clientName(handle.getBean()), handle.get()));
        }
        result.sort((a, b) -> a.name().compareTo(b.name()));
        return result;
    }

    private ClientInfo describe(String name, RemoteCacheManager manager) {
        List<String> servers = manager.getServers() == null ? List.of() : Arrays.asList(manager.getServers());
        boolean started = manager.isStarted();
        String protocolVersion = "";
        boolean statisticsEnabled = false;
        try {
            Configuration configuration = manager.getConfiguration();
            protocolVersion = String.valueOf(configuration.version());
            statisticsEnabled = configuration.statistics().enabled();
        } catch (RuntimeException e) {
            LOG.debugf(e, "Unable to read Infinispan client configuration for '%s'", name);
        }

        List<CacheInfo> caches = new ArrayList<>();
        try {
            for (String cacheName : new TreeSet<>(manager.getCacheNames())) {
                caches.add(describeCache(manager, cacheName));
            }
        } catch (RuntimeException e) {
            LOG.debugf(e, "Unable to list Infinispan caches for client '%s'", name);
        }

        return new ClientInfo(name, servers, started, protocolVersion, statisticsEnabled,
                manager.getActiveConnectionCount(), caches);
    }

    /**
     * Best-effort per-cache statistics. Server statistics require a live
     * connection and statistics enabled on the server; any failure (or missing
     * statistic) is logged and reported as -1 so the rest of the view still
     * renders.
     */
    private CacheInfo describeCache(RemoteCacheManager manager, String cacheName) {
        try {
            RemoteCache<?, ?> cache = manager.getCache(cacheName);
            if (cache != null) {
                ServerStatistics stats = cache.serverStatistics();
                if (stats != null) {
                    return new CacheInfo(cacheName,
                            stat(stats, ServerStatistics.CURRENT_NR_OF_ENTRIES),
                            stat(stats, ServerStatistics.HITS),
                            stat(stats, ServerStatistics.MISSES),
                            stat(stats, ServerStatistics.STORES),
                            stat(stats, ServerStatistics.RETRIEVALS),
                            stat(stats, ServerStatistics.REMOVE_HITS),
                            stat(stats, ServerStatistics.REMOVE_MISSES));
                }
            }
        } catch (RuntimeException e) {
            LOG.debugf(e, "Unable to read server statistics for cache '%s'", cacheName);
        }
        return new CacheInfo(cacheName, -1, -1, -1, -1, -1, -1, -1);
    }

    private static long stat(ServerStatistics stats, String key) {
        try {
            String value = stats.getStatistic(key);
            return value == null ? -1 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String clientName(Bean<?> bean) {
        if (bean != null) {
            for (Object qualifier : bean.getQualifiers()) {
                if (qualifier instanceof InfinispanClientName) {
                    return ((InfinispanClientName) qualifier).value();
                }
            }
        }
        return InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME;
    }

    public record CacheInfo(String name, long entries, long hits, long misses, long stores, long retrievals,
            long removeHits, long removeMisses) {
    }

    public record ClientInfo(String name, List<String> servers, boolean started, String protocolVersion,
            boolean statisticsEnabled, int activeConnections, List<CacheInfo> caches) {
    }
}
