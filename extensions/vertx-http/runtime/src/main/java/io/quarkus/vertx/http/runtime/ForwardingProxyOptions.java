package io.quarkus.vertx.http.runtime;

import java.util.List;

import io.netty.util.AsciiString;
import io.quarkus.vertx.http.runtime.TrustedProxyCheck.TrustedProxyCheckBuilder;
import io.quarkus.vertx.http.runtime.TrustedProxyCheck.TrustedProxyCheckPart;

public class ForwardingProxyOptions {
    final boolean proxyAddressForwarding;
    final boolean allowForwarded;
    final boolean allowXForwarded;
    final boolean enableForwardedHost;
    final boolean enableForwardedPrefix;
    final AsciiString forwardedHostHeader;
    final AsciiString forwardedPrefixHeader;
    final TrustedProxyCheckBuilder trustedProxyCheckBuilder;

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

    public static ForwardingProxyOptions from(HttpConfiguration httpConfiguration) {
        final boolean proxyAddressForwarding = httpConfiguration.proxy.proxyAddressForwarding;
        final boolean allowForwarded = httpConfiguration.proxy.allowForwarded;
        final boolean allowXForwarded = httpConfiguration.proxy.allowXForwarded.orElse(!allowForwarded);

        final boolean enableForwardedHost = httpConfiguration.proxy.enableForwardedHost;
        final boolean enableForwardedPrefix = httpConfiguration.proxy.enableForwardedPrefix;
        final AsciiString forwardedPrefixHeader = AsciiString.cached(httpConfiguration.proxy.forwardedPrefixHeader);
        final AsciiString forwardedHostHeader = AsciiString.cached(httpConfiguration.proxy.forwardedHostHeader);

        final List<TrustedProxyCheckPart> parts = httpConfiguration.proxy.trustedProxies
                .isPresent() ? List.copyOf(httpConfiguration.proxy.trustedProxies.get()) : List.of();
        final var proxyCheckBuilder = (!allowXForwarded && !allowForwarded)
                || parts.isEmpty() ? null : TrustedProxyCheckBuilder.builder(parts);

        return new ForwardingProxyOptions(proxyAddressForwarding, allowForwarded, allowXForwarded, enableForwardedHost,
                forwardedHostHeader, enableForwardedPrefix, forwardedPrefixHeader, proxyCheckBuilder);
    }
}
