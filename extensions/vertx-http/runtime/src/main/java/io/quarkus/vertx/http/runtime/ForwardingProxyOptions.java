package io.quarkus.vertx.http.runtime;

import java.util.List;

import io.netty.util.AsciiString;
import io.quarkus.vertx.http.runtime.TrustedProxyCheck.TrustedProxyCheckBuilder;
import io.quarkus.vertx.http.runtime.TrustedProxyCheck.TrustedProxyCheckPart;

public class ForwardingProxyOptions {
    public final boolean proxyAddressForwarding;
    final boolean allowForwarded;
    final boolean allowXForwarded;
    final boolean enableForwardedHost;
    final boolean enableForwardedPrefix;
    final AsciiString forwardedHostHeader;
    final AsciiString forwardedPrefixHeader;
    public final TrustedProxyCheckBuilder trustedProxyCheckBuilder;

    public ForwardingProxyOptions(final boolean proxyAddressForwarding,
            final boolean allowForwarded,
            final boolean allowXForwarded,
            final boolean enableForwardedHost,
            final AsciiString forwardedHostHeader,
            final boolean enableForwardedPrefix,
            final AsciiString forwardedPrefixHeader,
            TrustedProxyCheckBuilder trustedProxyCheckBuilder) {
        this.proxyAddressForwarding = proxyAddressForwarding;
        this.allowForwarded = allowForwarded;
        this.allowXForwarded = allowXForwarded;
        this.enableForwardedHost = enableForwardedHost;
        this.enableForwardedPrefix = enableForwardedPrefix;
        this.forwardedHostHeader = forwardedHostHeader;
        this.forwardedPrefixHeader = forwardedPrefixHeader;
        this.trustedProxyCheckBuilder = trustedProxyCheckBuilder;
    }

    public static ForwardingProxyOptions from(ProxyConfig proxy) {
        final boolean proxyAddressForwarding = proxy.proxyAddressForwarding;
        final boolean allowForwarded = proxy.allowForwarded;
        final boolean allowXForwarded = proxy.allowXForwarded.orElse(!allowForwarded);

        final boolean enableForwardedHost = proxy.enableForwardedHost;
        final boolean enableForwardedPrefix = proxy.enableForwardedPrefix;
        final AsciiString forwardedPrefixHeader = AsciiString.cached(proxy.forwardedPrefixHeader);
        final AsciiString forwardedHostHeader = AsciiString.cached(proxy.forwardedHostHeader);

        final List<TrustedProxyCheckPart> parts = proxy.trustedProxies
                .isPresent() ? List.copyOf(proxy.trustedProxies.get()) : List.of();
        final var proxyCheckBuilder = (!allowXForwarded && !allowForwarded)
                || parts.isEmpty() ? null : TrustedProxyCheckBuilder.builder(parts);

        return new ForwardingProxyOptions(proxyAddressForwarding, allowForwarded, allowXForwarded, enableForwardedHost,
                forwardedHostHeader, enableForwardedPrefix, forwardedPrefixHeader, proxyCheckBuilder);
    }
}
