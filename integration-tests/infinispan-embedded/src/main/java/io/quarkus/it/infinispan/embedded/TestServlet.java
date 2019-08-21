package io.quarkus.it.infinispan.embedded;

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
        byte[] result = cache.get(id.getBytes());
        return result == null ? "null" : new String(result);
    }

    @Transactional
    @Path("PUT/{cacheName}/{id}/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String put(@PathParam("cacheName") String cacheName, @PathParam("id") String id, @PathParam("value") String value,
            @QueryParam("shouldFail") String shouldFail) {
        log.info("Putting " + id + " with value: " + value + " into " + cacheName);
        Cache<byte[], byte[]> cache = emc.getCache(cacheName);
        byte[] result = cache.put(id.getBytes(), value.getBytes());
        if (Boolean.parseBoolean(shouldFail)) {
            throw new RuntimeException("Forced Exception!");
        }
        return result == null ? "null" : new String(result);
    }

    @Path("REMOVE/{cacheName}/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String remove(@PathParam("cacheName") String cacheName, @PathParam("id") String id) {
        log.info("Removing " + id + " from " + cacheName);
        Cache<byte[], byte[]> cache = emc.getCache(cacheName);
        byte[] result = cache.remove(id.getBytes());
        return result == null ? "null" : new String(result);
    }
}
