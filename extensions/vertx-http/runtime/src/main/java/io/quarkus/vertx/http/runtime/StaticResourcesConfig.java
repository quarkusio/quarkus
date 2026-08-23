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
     * How a request for a directory containing an index page is handled when its path does not end with a slash,
     * e.g. {@code /foo} when {@code /foo/index.html} exists:
     * <ul>
     * <li>{@code none}: nothing is served for that path</li>
     * <li>{@code redirect}: the request is redirected to the path ending with a slash, so that relative links in the
     * index page resolve against the directory</li>
     * <li>{@code reroute}: the index page is served for that path</li>
     * </ul>
     */
    @WithDefault("none")
    IndexDirectories normalizeIndexDirectories();

    enum IndexDirectories {
        NONE,
        REDIRECT,
        REROUTE
    }

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

    /**
     * Set whether directory listing should be enabled.
     * <p>
     * When enabled, if a request is made to a path that maps to a directory,
     * the server returns an HTML page listing the directory contents.
     */
    @WithDefault("false")
    boolean directoryListing();

    /**
     * Set whether the {@code Vary} header should be sent in responses.
     * <p>
     * The {@code Vary} header indicates to caches that the response may vary
     * based on the value of the specified request headers (e.g. {@code Accept-Encoding}).
     */
    @WithDefault("true")
    boolean sendVaryHeader();
}
