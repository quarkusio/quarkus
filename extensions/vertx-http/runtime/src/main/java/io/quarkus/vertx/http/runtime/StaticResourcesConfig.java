package io.quarkus.vertx.http.runtime;

import java.nio.charset.Charset;
import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class StaticResourcesConfig {

    /**
     * Set the index page when serving static resources.
     */
    @ConfigItem(defaultValue = "index.html")
    public String indexPage;

    /**
     * Set whether hidden files should be served.
     */
    @ConfigItem(defaultValue = "true")
    public boolean includeHidden;

    /**
     * Set whether range requests (resumable downloads; media streaming) should be enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableRangeSupport;

    /**
     * Set whether cache handling is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean cachingEnabled;

    /**
     * Set the cache entry timeout. The default is {@code 30} seconds.
     */
    @ConfigItem(defaultValue = "30S")
    public Duration cacheEntryTimeout;

    /**
     * Set value for max age in caching headers. The default is {@code 24} hours.
     */
    @ConfigItem(defaultValue = "24H")
    public Duration maxAge;

    /**
     * Set the max cache size.
     */
    @ConfigItem(defaultValue = "10000")
    public int maxCacheSize;

    /**
     * Content encoding for text related files
     */
    @ConfigItem(defaultValue = "UTF-8")
    public Charset contentEncoding;

}
