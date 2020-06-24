package io.quarkus.vertx.http.runtime;

import io.netty.util.AsciiString;

public class ForwardingProxyOptions {
    boolean proxyAddressForwarding;
    boolean allowForwarded;
    boolean enableForwardedHost;
    AsciiString forwardedHostHeader;

    public ForwardingProxyOptions(final boolean proxyAddressForwarding,
            final boolean allowForwarded,
            final boolean enableForwardedHost,
            final AsciiString forwardedHostHeader) {
        this.proxyAddressForwarding = proxyAddressForwarding;
        this.allowForwarded = allowForwarded;
        this.enableForwardedHost = enableForwardedHost;
        this.forwardedHostHeader = forwardedHostHeader;
    }

    public static ForwardingProxyOptions from(HttpConfiguration httpConfiguration) {
        final boolean proxyAddressForwarding = httpConfiguration.proxyAddressForwarding
                .orElse(httpConfiguration.proxy.proxyAddressForwarding);
        final boolean allowForwarded = httpConfiguration.allowForwarded
                .orElse(httpConfiguration.proxy.allowForwarded);

        final boolean enableForwardedHost = httpConfiguration.proxy.enableForwardedHost;
        final AsciiString forwardedHostHeader = AsciiString.cached(httpConfiguration.proxy.forwardedHostHeader);

        return new ForwardingProxyOptions(proxyAddressForwarding, allowForwarded, enableForwardedHost, forwardedHostHeader);
    }
}
