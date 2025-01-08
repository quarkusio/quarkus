package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.vertx.http.runtime.TrustedProxyCheck.TrustedProxyCheckPart;

/**
 * Holds configuration related with proxy addressing forward.
 */
@ConfigGroup
public class ProxyConfig {
    /**
     * Set whether the server should use the HA {@code PROXY} protocol when serving requests from behind a proxy.
     * (see the <a href="https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt">PROXY Protocol</a>).
     * When set to {@code true}, the remote address returned will be the one from the actual connecting client.
     * If it is set to {@code false} (default), the remote address returned will be the one from the proxy.
     */
    @ConfigItem(defaultValue = "false")
    public boolean useProxyProtocol;

    /**
     * If this is true then the address, scheme etc. will be set from headers forwarded by the proxy server, such as
     * {@code X-Forwarded-For}. This should only be set if you are behind a proxy that sets these headers.
     */
    @ConfigItem
    public boolean proxyAddressForwarding;

    /**
     * If this is true and proxy address forwarding is enabled then the standard {@code Forwarded} header will be used.
     * In case the not standard {@code X-Forwarded-For} header is enabled and detected on HTTP requests, the standard header has
     * the precedence.
     * Activating this together with {@code quarkus.http.proxy.allow-x-forwarded} has security implications as clients can forge
     * requests with a forwarded header that is not overwritten by the proxy. Therefore, proxies should strip unexpected
     * `Forwarded` or `X-Forwarded-*` headers from the client.
     */
    @ConfigItem
    public boolean allowForwarded;

    /**
     * If either this or {@code allow-forwarded} are true and proxy address forwarding is enabled then the not standard
     * {@code Forwarded} header will be used.
     * In case the standard {@code Forwarded} header is enabled and detected on HTTP requests, the standard header has the
     * precedence.
     * Activating this together with {@code quarkus.http.proxy.allow-forwarded} has security implications as clients can forge
     * requests with a forwarded header that is not overwritten by the proxy. Therefore, proxies should strip unexpected
     * `Forwarded` or `X-Forwarded-*` headers from the client.
     */
    @ConfigItem
    public Optional<Boolean> allowXForwarded;

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

    /**
     * Adds the header `X-Forwarded-Trusted-Proxy` if the request is forwarded by a trusted proxy.
     * The value is `true` if the request is forwarded by a trusted proxy, otherwise `null`.
     * <p>
     * The forwarded parser detects forgery attempts and if the incoming request contains this header, it will be removed
     * from the request.
     * <p>
     * The `X-Forwarded-Trusted-Proxy` header is a custom header, not part of the standard `Forwarded` header.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableTrustedProxyHeader;

    /**
     * Configure the list of trusted proxy addresses.
     * Received `Forwarded`, `X-Forwarded` or `X-Forwarded-*` headers from any other proxy address will be ignored.
     * The trusted proxy address should be specified as the IP address (IPv4 or IPv6), hostname or Classless Inter-Domain
     * Routing (CIDR) notation. Please note that Quarkus needs to perform DNS lookup for all hostnames during the request.
     * For that reason, using hostnames is not recommended.
     * <p>
     * Examples of a socket address in the form of `host` or `host:port`:
     *
     * <ul>
     * <li>`127.0.0.1:8084`</li>
     * <li>`[0:0:0:0:0:0:0:1]`</li>
     * <li>`[0:0:0:0:0:0:0:1]:8084`</li>
     * <li>`[::]`</li>
     * <li>`localhost`</li>
     * <li>`localhost:8084`</li>
     * </ul>
     * <p>
     * Examples of a CIDR notation:
     *
     * <ul>
     * <li>`::/128`</li>
     * <li>`::/0`</li>
     * <li>`127.0.0.0/8`</li>
     * </ul>
     * <p>
     * Please bear in mind that IPv4 CIDR won't match request sent from the IPv6 address and the other way around.
     */
    @ConfigItem(defaultValueDocumentation = "All proxy addresses are trusted")
    @ConvertWith(TrustedProxyCheckPartConverter.class)
    public Optional<List<TrustedProxyCheckPart>> trustedProxies;

}
