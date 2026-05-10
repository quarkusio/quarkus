package io.quarkus.vertx.http.runtime;

import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import io.netty.util.AsciiString;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.ProxyConfig.ForwardedPrecedence;
import io.quarkus.vertx.http.runtime.TrustedProxyCheck.TrustedProxyCheckBuilder;
import io.quarkus.vertx.http.runtime.TrustedProxyCheck.TrustedProxyCheckPart;
import io.vertx.core.http.ClientAuth;

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
    public final List<List<Rdn>> trustedProxyDns;

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
            TrustedProxyCheckBuilder trustedProxyCheckBuilder,
            List<List<Rdn>> trustedProxyDns) {
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
        this.trustedProxyDns = trustedProxyDns;
    }

    public static ForwardingProxyOptions from(ProxyConfig proxyConfig, ClientAuth clientAuth) {
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

        final List<List<Rdn>> trustedProxyDns = collectAndValidateTrustedProxyDns(proxyConfig, clientAuth, parts,
                allowXForwarded, allowForwarded);

        final var proxyCheckBuilder = (!allowXForwarded && !allowForwarded)
                || parts.isEmpty() ? null : TrustedProxyCheckBuilder.builder(parts);

        return new ForwardingProxyOptions(proxyAddressForwarding, allowForwarded, allowXForwarded, enableForwardedHost,
                enableTrustedProxyHeader, forwardedHostHeader, enableForwardedPrefix, strictForwardedControl,
                forwardedPrecedence, forwardedPrefixHeader, proxyCheckBuilder, trustedProxyDns);
    }

    private static List<List<Rdn>> collectAndValidateTrustedProxyDns(ProxyConfig proxyConfig, ClientAuth clientAuth,
            List<TrustedProxyCheckPart> parts, boolean allowXForwarded, boolean allowForwarded) {
        final List<String> dnStrings = proxyConfig.trustedProxyDns().orElse(List.of());
        if (dnStrings.isEmpty()) {
            return null;
        }

        if (!parts.isEmpty()) {
            throw new ConfigurationException(
                    "'quarkus.http.proxy.trusted-proxies' and 'quarkus.http.proxy.trusted-proxy-dns' are mutually exclusive");
        }

        if (clientAuth == ClientAuth.NONE) {
            throw new ConfigurationException(
                    "'quarkus.http.proxy.trusted-proxy-dns' requires 'quarkus.http.ssl.client-auth' to be set "
                            + "to 'request' or 'required'");
        }

        if (!allowXForwarded && !allowForwarded) {
            throw new ConfigurationException(
                    "'quarkus.http.proxy.trusted-proxy-dns' requires 'quarkus.http.proxy.allow-forwarded' "
                            + "or 'quarkus.http.proxy.allow-x-forwarded' to be enabled");
        }

        return dnStrings.stream().map(dn -> {
            try {
                var x500PrincipalName = new X500Principal(dn).getName(); // force DN validation
                return List.copyOf(new LdapName(x500PrincipalName).getRdns());
            } catch (IllegalArgumentException | InvalidNameException e) {
                throw new ConfigurationException("Invalid 'quarkus.http.proxy.trusted-proxy-dns' value '" + dn
                        + "': not a valid RFC 2253 Distinguished Name", e);
            }
        }).toList();
    }
}
