package io.quarkus.vertx.http.runtime;

import java.nio.charset.Charset;
import java.time.Duration;

import io.smallrye.config.WithDefault;

public interface StaticResourcesConfig {
    /**
     * Set the index page when serving static resources.
     */
    @WithDefault("index.html")
    String indexPage();

    /**
     * Set whether hidden files should be served.
     */
    @WithDefault("true")
    boolean includeHidden();

    /**
     * Set whether range requests (resumable downloads; media streaming) should be enabled.
     */
    @WithDefault("true")
    boolean enableRangeSupport();

    /**
     * Set whether cache handling is enabled.
     */
    @WithDefault("true")
    boolean cachingEnabled();

    /**
     * Set the cache entry timeout. The default is {@code 30} seconds.
     */
    @WithDefault("30S")
    Duration cacheEntryTimeout();

    /**
     * Set value for max age in caching headers. The default is {@code 24} hours.
     */
    @WithDefault("24H")
    Duration maxAge();

    /**
     * Set the max cache size.
     */
    @WithDefault("10000")
    int maxCacheSize();

    /**
     * Content encoding for text related files
     */
    @WithDefault("UTF-8")
    Charset contentEncoding();
}
