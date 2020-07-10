package io.quarkus.vertx.http.runtime;

import io.netty.util.AsciiString;

public class ForwardingProxyOptions {
    boolean proxyAddressForwarding;
    boolean allowForwarded;
    boolean enableForwardedHost;
    boolean enableForwardedPrefix;
    AsciiString forwardedHostHeader;
    AsciiString forwardedPrefixHeader;

    public ForwardingProxyOptions(final boolean proxyAddressForwarding,
            final boolean allowForwarded,
            final boolean enableForwardedHost,
            final AsciiString forwardedHostHeader,
            final boolean enableForwardedPrefix,
            final AsciiString forwardedPrefixHeader) {
        this.proxyAddressForwarding = proxyAddressForwarding;
        this.allowForwarded = allowForwarded;
        this.enableForwardedHost = enableForwardedHost;
        this.enableForwardedPrefix = enableForwardedPrefix;
        this.forwardedHostHeader = forwardedHostHeader;
        this.forwardedPrefixHeader = forwardedPrefixHeader;
    }

    public static ForwardingProxyOptions from(HttpConfiguration httpConfiguration) {
        final boolean proxyAddressForwarding = httpConfiguration.proxyAddressForwarding
                .orElse(httpConfiguration.proxy.proxyAddressForwarding);
        final boolean allowForwarded = httpConfiguration.allowForwarded
                .orElse(httpConfiguration.proxy.allowForwarded);

        final boolean enableForwardedHost = httpConfiguration.proxy.enableForwardedHost;
        final boolean enableForwardedPrefix = httpConfiguration.proxy.enableForwardedPrefix;
        final AsciiString forwardedPrefixHeader = AsciiString.cached(httpConfiguration.proxy.forwardedPrefixHeader);
        final AsciiString forwardedHostHeader = AsciiString.cached(httpConfiguration.proxy.forwardedHostHeader);

        return new ForwardingProxyOptions(proxyAddressForwarding, allowForwarded, enableForwardedHost, forwardedHostHeader,
                enableForwardedPrefix, forwardedPrefixHeader);
    }
}
