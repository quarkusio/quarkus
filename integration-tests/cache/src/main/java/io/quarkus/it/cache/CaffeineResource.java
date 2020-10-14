package io.quarkus.it.cache;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.github.benmanes.caffeine.cache.CacheLoader;

@ApplicationScoped
@Path("/caffeine")
public class CaffeineResource {
    /*
     * Replicates the behavior of com.github.benmanes.caffeine.cache.LocalLoadingCache.java
     *
     * https://github.com/ben-manes/caffeine/blob/37f6ad303eb4474cd8a644551d528dfda37c5bfc/caffeine/src/main/java/com/github/
     * benmanes/caffeine/cache/LocalLoadingCache.java#L176-L185
     */
    public static boolean hasLoadAll(CacheLoader<String, String> cl) {
        try {
            Method classLoadAll = cl.getClass().getMethod("loadAll", Iterable.class);
            Method defaultLoadAll = CacheLoader.class.getMethod("loadAll", Iterable.class);
            return !classLoadAll.equals(defaultLoadAll);
        } catch (NoSuchMethodException | SecurityException e) {
            return false;
        }
    }

    @GET
    @Path("/hasLoadAll")
    public JsonObject hasLoadAll() {
        return Json.createObjectBuilder()
                .add("loader", hasLoadAll(new MyCacheLoader()))
                .add("bulk-loader", hasLoadAll(new MyBulkCacheLoader()))
                .build();
    }

    public static class MyCacheLoader implements CacheLoader<String, String> {
        @Override
        public String load(String unused) throws Exception {
            return null;
        }
    }

    public static class MyBulkCacheLoader implements CacheLoader<String, String> {
        @Override
        public String load(String unused) throws Exception {
            return null;
        }

        @Override
        public Map<String, String> loadAll(Iterable<? extends String> unused) throws Exception {
            return Collections.emptyMap();
        }
    }
}
