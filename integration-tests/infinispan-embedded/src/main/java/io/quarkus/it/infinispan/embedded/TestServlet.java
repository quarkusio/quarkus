package io.quarkus.it.infinispan.embedded;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import io.quarkus.runtime.StartupEvent;

@Path("/test")
public class TestServlet {
    private static final Log log = LogFactory.getLog(TestServlet.class);

    @Inject
    EmbeddedCacheManager emc;

    // Having on start method will eagerly initialize the cache manager which in turn starts up clustered cache
    void onStart(@Observes StartupEvent ev) {
        log.info("The application is starting...");
    }

    @Path("GET/{cacheName}/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@PathParam("cacheName") String cacheName, @PathParam("id") String id) {
        log.info("Retrieving " + id + " from " + cacheName);
        Cache<byte[], byte[]> cache = emc.getCache(cacheName);
        byte[] result = cache.get(id.getBytes(StandardCharsets.UTF_8));
        return result == null ? "null" : new String(result, StandardCharsets.UTF_8);
    }

    @Transactional
    @Path("PUT/{cacheName}/{id}/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String put(@PathParam("cacheName") String cacheName, @PathParam("id") String id, @PathParam("value") String value,
            @QueryParam("shouldFail") String shouldFail) {
        log.info("Putting " + id + " with value: " + value + " into " + cacheName);
        Cache<byte[], byte[]> cache = emc.getCache(cacheName);
        byte[] result = cache.put(id.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
        if (Boolean.parseBoolean(shouldFail)) {
            throw new RuntimeException("Forced Exception!");
        }
        return result == null ? "null" : new String(result, StandardCharsets.UTF_8);
    }

    @Path("REMOVE/{cacheName}/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String remove(@PathParam("cacheName") String cacheName, @PathParam("id") String id) {
        log.info("Removing " + id + " from " + cacheName);
        Cache<byte[], byte[]> cache = emc.getCache(cacheName);
        byte[] result = cache.remove(id.getBytes(StandardCharsets.UTF_8));
        return result == null ? "null" : new String(result, StandardCharsets.UTF_8);
    }

    @Path("CLUSTER")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String simpleCluster() throws IOException {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.clustering().cacheMode(CacheMode.DIST_SYNC);

        List<EmbeddedCacheManager> managers = new ArrayList<>(3);
        try {
            // Force TCP to connect to loopback, which our TCPPING in dist.xml connects to for discovery
            String oldProperty = System.setProperty("jgroups.tcp.address", "127.0.0.1");
            for (int i = 0; i < 3; i++) {
                EmbeddedCacheManager ecm = new DefaultCacheManager(
                        Paths.get("src", "main", "resources", "dist.xml").toString());
                ecm.start();
                managers.add(ecm);
                // Start the default cache
                ecm.getCache();
            }

            if (oldProperty != null) {
                System.setProperty("jgroups.tcp.address", oldProperty);
            }

            long failureTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);

            int sizeMatched = 0;
            while (sizeMatched < 3) {
                // reset the size every time
                sizeMatched = 0;
                for (EmbeddedCacheManager ecm : managers) {
                    int size = ecm.getMembers().size();
                    if (size == 3) {
                        sizeMatched++;
                    }
                }
                if (failureTime - System.nanoTime() < 0) {
                    return "Timed out waiting for caches to have joined together!";
                }
            }
        } finally {
            managers.forEach(EmbeddedCacheManager::stop);
        }
        return "Success";
    }
}
