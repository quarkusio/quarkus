package io.quarkus.it.cache;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
    public Result hasLoadAll() {
        return new Result(hasLoadAll(new MyCacheLoader()),
                hasLoadAll(new MyBulkCacheLoader()));
    }

    public static class Result {

        private final boolean loader;
        private final boolean bulkLoader;

        Result(boolean loader, boolean bulkLoader) {
            this.loader = loader;
            this.bulkLoader = bulkLoader;
        }

        public boolean isLoader() {
            return loader;
        }

        public boolean isBulkLoader() {
            return bulkLoader;
        }
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
