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
    final boolean validateForwardedProto;
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

    private ForwardingProxyOptions(Builder builder) {
        this.proxyAddressForwarding = builder.proxyAddressForwarding;
        this.validateForwardedProto = builder.validateForwardedProto;
        this.allowForwarded = builder.allowForwarded;
        this.allowXForwarded = builder.allowXForwarded;
        this.enableForwardedHost = builder.enableForwardedHost;
        this.enableForwardedPrefix = builder.enableForwardedPrefix;
        this.forwardedHostHeader = builder.forwardedHostHeader;
        this.forwardedPrefixHeader = builder.forwardedPrefixHeader;
        this.strictForwardedControl = builder.strictForwardedControl;
        this.forwardedPrecedence = builder.forwardedPrecedence;
        this.trustedProxyCheckBuilder = builder.trustedProxyCheckBuilder;
        this.enableTrustedProxyHeader = builder.enableTrustedProxyHeader;
        this.trustedProxyDns = builder.trustedProxyDns;
    }

    static Builder builder() {
        return new Builder();
    }

    public static ForwardingProxyOptions from(ProxyConfig proxyConfig, ClientAuth clientAuth) {
        final boolean allowForwarded = proxyConfig.allowForwarded();
        final boolean allowXForwarded = proxyConfig.allowXForwarded().orElse(!allowForwarded);

        final List<TrustedProxyCheckPart> parts = proxyConfig.trustedProxies()
                .isPresent() ? List.copyOf(proxyConfig.trustedProxies().get()) : List.of();

        final List<List<Rdn>> trustedProxyDns = collectAndValidateTrustedProxyDns(proxyConfig, clientAuth, parts,
                allowXForwarded, allowForwarded);

        final var proxyCheckBuilder = (!allowXForwarded && !allowForwarded)
                || parts.isEmpty() ? null : TrustedProxyCheckBuilder.builder(parts);

        return builder()
                .proxyAddressForwarding(proxyConfig.proxyAddressForwarding())
                .validateForwardedProto(
                        proxyConfig.forwardedProtoValidation() == ProxyConfig.ForwardedProtoValidation.REJECT)
                .allowForwarded(allowForwarded)
                .allowXForwarded(allowXForwarded)
                .enableForwardedHost(proxyConfig.enableForwardedHost())
                .enableForwardedPrefix(proxyConfig.enableForwardedPrefix())
                .enableTrustedProxyHeader(proxyConfig.enableTrustedProxyHeader())
                .strictForwardedControl(proxyConfig.strictForwardedControl())
                .forwardedPrecedence(proxyConfig.forwardedPrecedence())
                .forwardedHostHeader(AsciiString.cached(proxyConfig.forwardedHostHeader()))
                .forwardedPrefixHeader(AsciiString.cached(proxyConfig.forwardedPrefixHeader()))
                .trustedProxyCheckBuilder(proxyCheckBuilder)
                .trustedProxyDns(trustedProxyDns)
                .build();
    }

    private static List<List<Rdn>> collectAndValidateTrustedProxyDns(ProxyConfig proxyConfig, ClientAuth clientAuth,
            List<TrustedProxyCheckPart> parts, boolean allowXForwarded, boolean allowForwarded) {
        final List<String> dnStrings = proxyConfig.trustedProxy().stream()
                .map(ProxyConfig.TrustedProxyConfig::subjectDn)
                .toList();
        if (dnStrings.isEmpty()) {
            return null;
        }

        if (!parts.isEmpty()) {
            throw new ConfigurationException(
                    "'quarkus.http.proxy.trusted-proxies' and 'quarkus.http.proxy.trusted-proxy[*].subject-dn' are mutually exclusive");
        }

        if (clientAuth == ClientAuth.NONE) {
            throw new ConfigurationException(
                    "'quarkus.http.proxy.trusted-proxy[*].subject-dn' requires 'quarkus.http.ssl.client-auth' to be set "
                            + "to 'request' or 'required'");
        }

        if (!allowXForwarded && !allowForwarded) {
            throw new ConfigurationException(
                    "'quarkus.http.proxy.trusted-proxy[*].subject-dn' requires 'quarkus.http.proxy.allow-forwarded' "
                            + "or 'quarkus.http.proxy.allow-x-forwarded' to be enabled");
        }

        return dnStrings.stream().map(dn -> {
            try {
                var x500PrincipalName = new X500Principal(dn).getName(); // force DN validation
                return List.copyOf(new LdapName(x500PrincipalName).getRdns());
            } catch (IllegalArgumentException | InvalidNameException e) {
                throw new ConfigurationException("Invalid 'quarkus.http.proxy.trusted-proxy[*].subject-dn' value '" + dn
                        + "': not a valid RFC 2253 Distinguished Name", e);
            }
        }).toList();
    }

    static final class Builder {

        private boolean proxyAddressForwarding;
        private boolean validateForwardedProto;
        private boolean allowForwarded;
        private boolean allowXForwarded;
        private boolean enableForwardedHost;
        private boolean enableForwardedPrefix;
        private AsciiString forwardedHostHeader;
        private AsciiString forwardedPrefixHeader;
        private boolean strictForwardedControl;
        private ForwardedPrecedence forwardedPrecedence;
        private TrustedProxyCheckBuilder trustedProxyCheckBuilder;
        private boolean enableTrustedProxyHeader;
        private List<List<Rdn>> trustedProxyDns;

        private Builder() {
        }

        Builder proxyAddressForwarding(boolean proxyAddressForwarding) {
            this.proxyAddressForwarding = proxyAddressForwarding;
            return this;
        }

        Builder validateForwardedProto(boolean validateForwardedProto) {
            this.validateForwardedProto = validateForwardedProto;
            return this;
        }

        Builder allowForwarded(boolean allowForwarded) {
            this.allowForwarded = allowForwarded;
            return this;
        }

        Builder allowXForwarded(boolean allowXForwarded) {
            this.allowXForwarded = allowXForwarded;
            return this;
        }

        Builder enableForwardedHost(boolean enableForwardedHost) {
            this.enableForwardedHost = enableForwardedHost;
            return this;
        }

        Builder enableForwardedPrefix(boolean enableForwardedPrefix) {
            this.enableForwardedPrefix = enableForwardedPrefix;
            return this;
        }

        Builder forwardedHostHeader(AsciiString forwardedHostHeader) {
            this.forwardedHostHeader = forwardedHostHeader;
            return this;
        }

        Builder forwardedPrefixHeader(AsciiString forwardedPrefixHeader) {
            this.forwardedPrefixHeader = forwardedPrefixHeader;
            return this;
        }

        Builder strictForwardedControl(boolean strictForwardedControl) {
            this.strictForwardedControl = strictForwardedControl;
            return this;
        }

        Builder forwardedPrecedence(ForwardedPrecedence forwardedPrecedence) {
            this.forwardedPrecedence = forwardedPrecedence;
            return this;
        }

        Builder trustedProxyCheckBuilder(TrustedProxyCheckBuilder trustedProxyCheckBuilder) {
            this.trustedProxyCheckBuilder = trustedProxyCheckBuilder;
            return this;
        }

        Builder enableTrustedProxyHeader(boolean enableTrustedProxyHeader) {
            this.enableTrustedProxyHeader = enableTrustedProxyHeader;
            return this;
        }

        Builder trustedProxyDns(List<List<Rdn>> trustedProxyDns) {
            this.trustedProxyDns = trustedProxyDns;
            return this;
        }

        ForwardingProxyOptions build() {
            return new ForwardingProxyOptions(this);
        }
    }
}
