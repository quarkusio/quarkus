package io.quarkus.cache;

import java.util.Set;
import java.util.function.Supplier;

public interface CacheManagerInfo {

    boolean supports(Context context);

    Supplier<CacheManager> get(Context context);

    interface Context {

        boolean cacheEnabled();

        Metrics metrics();

        String cacheType();

        Set<String> cacheNames();

        enum Metrics {
            NONE,
            MICROMETER
        }
    }
}
