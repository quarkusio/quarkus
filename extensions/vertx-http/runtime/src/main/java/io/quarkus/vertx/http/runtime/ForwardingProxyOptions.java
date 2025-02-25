package io.quarkus.vertx.http.runtime;

import java.util.List;

import io.netty.util.AsciiString;
import io.quarkus.vertx.http.runtime.ProxyConfig.ForwardedPrecedence;
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
    final boolean strictForwardedControl;
    final ForwardedPrecedence forwardedPrecedence;
    public final TrustedProxyCheckBuilder trustedProxyCheckBuilder;
    final boolean enableTrustedProxyHeader;

    public ForwardingProxyOptions(final boolean proxyAddressForwarding,
            boolean allowForwarded,
            boolean allowXForwarded,
            boolean enableForwardedHost,
            boolean enableTrustedProxyHeader,
            AsciiString forwardedHostHeader,
            boolean enableForwardedPrefix,
            boolean strictForwardedControl,
            ForwardedPrecedence forwardedPrecedence,
            AsciiString forwardedPrefixHeader,
            TrustedProxyCheckBuilder trustedProxyCheckBuilder) {
        this.proxyAddressForwarding = proxyAddressForwarding;
        this.allowForwarded = allowForwarded;
        this.allowXForwarded = allowXForwarded;
        this.enableForwardedHost = enableForwardedHost;
        this.enableForwardedPrefix = enableForwardedPrefix;
        this.forwardedHostHeader = forwardedHostHeader;
        this.forwardedPrefixHeader = forwardedPrefixHeader;
        this.strictForwardedControl = strictForwardedControl;
        this.forwardedPrecedence = forwardedPrecedence;
        this.trustedProxyCheckBuilder = trustedProxyCheckBuilder;
        this.enableTrustedProxyHeader = enableTrustedProxyHeader;
    }

    public static ForwardingProxyOptions from(ProxyConfig proxyConfig) {
        final boolean proxyAddressForwarding = proxyConfig.proxyAddressForwarding();
        final boolean allowForwarded = proxyConfig.allowForwarded();
        final boolean allowXForwarded = proxyConfig.allowXForwarded().orElse(!allowForwarded);
        final boolean enableForwardedHost = proxyConfig.enableForwardedHost();
        final boolean enableForwardedPrefix = proxyConfig.enableForwardedPrefix();
        final boolean enableTrustedProxyHeader = proxyConfig.enableTrustedProxyHeader();
        final boolean strictForwardedControl = proxyConfig.strictForwardedControl();
        final ForwardedPrecedence forwardedPrecedence = proxyConfig.forwardedPrecedence();
        final AsciiString forwardedPrefixHeader = AsciiString.cached(proxyConfig.forwardedPrefixHeader());
        final AsciiString forwardedHostHeader = AsciiString.cached(proxyConfig.forwardedHostHeader());

        final List<TrustedProxyCheckPart> parts = proxyConfig.trustedProxies()
                .isPresent() ? List.copyOf(proxyConfig.trustedProxies().get()) : List.of();
        final var proxyCheckBuilder = (!allowXForwarded && !allowForwarded)
                || parts.isEmpty() ? null : TrustedProxyCheckBuilder.builder(parts);

        return new ForwardingProxyOptions(proxyAddressForwarding, allowForwarded, allowXForwarded, enableForwardedHost,
                enableTrustedProxyHeader, forwardedHostHeader, enableForwardedPrefix, strictForwardedControl,
                forwardedPrecedence, forwardedPrefixHeader, proxyCheckBuilder);
    }
}
