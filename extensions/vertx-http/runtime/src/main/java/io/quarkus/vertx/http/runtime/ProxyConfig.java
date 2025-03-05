package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.vertx.http.runtime.TrustedProxyCheck.TrustedProxyCheckPart;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * Holds configuration related with proxy addressing forward.
 */
public interface ProxyConfig {
    /**
     * Set whether the server should use the HA {@code PROXY} protocol when serving requests from behind a proxy.
     * (see the <a href="https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt">PROXY Protocol</a>).
     * When set to {@code true}, the remote address returned will be the one from the actual connecting client.
     * If it is set to {@code false} (default), the remote address returned will be the one from the proxy.
     */
    @WithDefault("false")
    boolean useProxyProtocol();

    /**
     * If this is true then the address, scheme etc. will be set from headers forwarded by the proxy server, such as
     * {@code X-Forwarded-For}. This should only be set if you are behind a proxy that sets these headers.
     */
    @WithDefault("false")
    boolean proxyAddressForwarding();

    /**
     * If this is true and proxy address forwarding is enabled then the standard {@code Forwarded} header will be used.
     * In case the not standard {@code X-Forwarded-For} header is enabled and detected on HTTP requests, the standard header has
     * the precedence.
     * Activating this together with {@code quarkus.http.proxy.allow-x-forwarded} has security implications as clients can forge
     * requests with a forwarded header that is not overwritten by the proxy. Therefore, proxies should strip unexpected
     * `Forwarded` or `X-Forwarded-*` headers from the client.
     */
    @WithDefault("false")
    boolean allowForwarded();

    /**
     * If either this or {@code allow-forwarded} are true and proxy address forwarding is enabled then the not standard
     * {@code Forwarded} header will be used.
     * In case the standard {@code Forwarded} header is enabled and detected on HTTP requests, the standard header has the
     * precedence.
     * Activating this together with {@code quarkus.http.proxy.allow-forwarded} has security implications as clients can forge
     * requests with a forwarded header that is not overwritten by the proxy. Therefore, proxies should strip unexpected
     * `Forwarded` or `X-Forwarded-*` headers from the client.
     */
    Optional<Boolean> allowXForwarded();

    /**
     * When both Forwarded and X-Forwarded headers are enabled with {@link #allowForwarded} and {@link #allowXForwarded}
     * respectively, enforce that the identical headers must have equal values.
     */
    @WithDefault("true")
    boolean strictForwardedControl();

    /**
     * Precedence of Forwarded and X-Forwarded headers when both types of headers are enabled and no strict forwarded control is
     * enforced.
     */
    enum ForwardedPrecedence {
        FORWARDED,
        X_FORWARDED
    }

    /**
     * When both Forwarded and X-Forwarded headers are enabled with {@link #allowForwarded} and {@link #allowXForwarded}
     * respectively, and {@link #strictForwardedControl} enforcing that the identical headers must have equal values is
     * disabled,
     * choose if it is Forwarded or X-Forwarded matching header value that is preferred.
     * <p>
     * For example, if Forwarded has a precedence over X-Forwarded, Forwarded scheme is `http` and X-Forwarded scheme is
     * `https`,
     * then the final scheme value is `http`. If X-Forwarded has a precedence, then the final scheme value is 'https'.
     */
    @WithDefault("forwarded")
    ForwardedPrecedence forwardedPrecedence();

    /**
     * Enable override the received request's host through a forwarded host header.
     */
    @WithDefault("false")
    boolean enableForwardedHost();

    /**
     * Configure the forwarded host header to be used if override enabled.
     */
    @WithDefault("X-Forwarded-Host")
    String forwardedHostHeader();

    /**
     * Enable prefix the received request's path with a forwarded prefix header.
     */
    @WithDefault("false")
    boolean enableForwardedPrefix();

    /**
     * Configure the forwarded prefix header to be used if prefixing enabled.
     */
    @WithDefault("X-Forwarded-Prefix")
    String forwardedPrefixHeader();

    /**
     * Adds the header `X-Forwarded-Trusted-Proxy` if the request is forwarded by a trusted proxy.
     * The value is `true` if the request is forwarded by a trusted proxy, otherwise `null`.
     * <p>
     * The forwarded parser detects forgery attempts and if the incoming request contains this header, it will be removed
     * from the request.
     * <p>
     * The `X-Forwarded-Trusted-Proxy` header is a custom header, not part of the standard `Forwarded` header.
     */
    @WithDefault("false")
    boolean enableTrustedProxyHeader();

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
    @ConfigDocDefault("All proxy addresses are trusted")
    Optional<List<@WithConverter(TrustedProxyCheckPartConverter.class) TrustedProxyCheckPart>> trustedProxies();
}
