package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Holds configuration related with proxy addressing forward.
 */
@ConfigGroup
public class ProxyConfig {
    /**
     * If this is true then the address, scheme etc will be set from headers forwarded by the proxy server, such as
     * {@code X-Forwarded-For}. This should only be set if you are behind a proxy that sets these headers.
     */
    @ConfigItem
    public boolean proxyAddressForwarding;

    /**
     * If this is true and proxy address forwarding is enabled then the standard {@code Forwarded} header will be used,
     * rather than the more common but not standard {@code X-Forwarded-For}.
     */
    @ConfigItem
    public boolean allowForwarded;

    /**
     * Enable override the received request's host through a forwarded host header.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableForwardedHost;

    /**
     * Configure the forwarded host header to be used if override enabled.
     */
    @ConfigItem(defaultValue = "X-Forwarded-Host")
    public String forwardedHostHeader;

    /**
     * Enable prefix the received request's path with a forwarded prefix header.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableForwardedPrefix;

    /**
     * Configure the forwarded prefix header to be used if prefixing enabled.
     */
    @ConfigItem(defaultValue = "X-Forwarded-Prefix")
    public String forwardedPrefixHeader;
}
