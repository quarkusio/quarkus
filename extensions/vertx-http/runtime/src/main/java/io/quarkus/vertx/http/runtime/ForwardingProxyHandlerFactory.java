package io.quarkus.vertx.http.runtime;

import static io.quarkus.vertx.http.runtime.TrustedProxyCheck.TrustedProxyCheckBuilder.builder;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.vertx.http.runtime.ProxyConfig.TrustedProxyConfig;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerRequest;

/**
 * Applies a {@link TrustedProxyCheck} based on {@link ProxyConfig} when the proxy address forwarding is enabled.
 */
public record ForwardingProxyHandlerFactory(ProxyConfig proxyConfig, ClientAuth clientAuth,
        Optional<String> tlsConfigName, Supplier<Vertx> vertx,
        Handler<HttpServerRequest> root, String configPrefix,
        boolean allowForwarded, boolean allowXForwarded,
        boolean hasClientAuthTrustCheck, boolean hasHostTrustCheck) {

    public ForwardingProxyHandlerFactory(ProxyConfig proxyConfig, ClientAuth clientAuth,
            Optional<String> tlsConfigName, Supplier<Vertx> vertx,
            Handler<HttpServerRequest> root, String configPrefix) {
        this(proxyConfig, clientAuth, tlsConfigName, vertx, root, configPrefix,
                proxyConfig.allowForwarded(),
                proxyConfig.allowXForwarded().orElse(!proxyConfig.allowForwarded()),
                !proxyConfig.trustedProxy().isEmpty(),
                proxyConfig.trustedProxies().filter(Predicate.not(List::isEmpty)).isPresent());
    }

    public Handler<HttpServerRequest> createHandler() {
        validateTrustedProxyConfig();

        ForwardingProxyOptions options = ForwardingProxyOptions.from(proxyConfig);
        if (hasHostTrustCheck) {
            return new ForwardedProxyHandler(builder(proxyConfig.trustedProxies().get()), vertx, root, options);
        } else if (hasClientAuthTrustCheck) {
            return new RequestForwardingProxyHandler(allowConfiguredClients(proxyConfig.trustedProxy()), root, options);
        }
        return new RequestForwardingProxyHandler(allowAll(), root, options);
    }

    private Function<HttpServerRequest, TrustedProxyCheck> allowConfiguredClients(
            List<TrustedProxyConfig> trustedProxyConfigs) {
        if (trustedProxyConfigs.stream().anyMatch(c -> c.subjectDn() == null || c.subjectDn().isEmpty())) {
            throw missingSubjectDn();
        }
        TrustManagerHolder holder = createTrustManagerHolderIfNeeded(trustedProxyConfigs);
        if (holder != null) {
            return checkBothRDNsAndTruststore(trustedProxyConfigs, holder);
        }
        return checkOnlyRDNs(trustedProxyConfigs);
    }

    private Function<HttpServerRequest, TrustedProxyCheck> checkOnlyRDNs(List<TrustedProxyConfig> trustedProxyConfigs) {
        return checkRDNs(trustedProxyConfigs.stream().map(TrustedProxyConfig::subjectDn).toList());
    }

    private Function<HttpServerRequest, TrustedProxyCheck> checkBothRDNsAndTruststore(
            List<TrustedProxyConfig> trustedProxyConfigs, TrustManagerHolder holder) {
        return trustedProxyConfigs.stream()
                .map(c -> {
                    var dnCheck = checkRDNs(List.of(c.subjectDn()));
                    if (c.truststoreAlias().isPresent()) {
                        var truststoreAliasCheck = holder.forAlias(c.truststoreAlias().get());
                        return logicalAnd(truststoreAliasCheck, dnCheck);
                    }
                    return dnCheck;
                })
                .reduce(ForwardingProxyHandlerFactory::logicalOr)
                .orElseThrow();
    }

    private TrustManagerHolder createTrustManagerHolderIfNeeded(List<TrustedProxyConfig> configs) {
        Set<String> aliases = configs.stream()
                .map(TrustedProxyConfig::truststoreAlias)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        if (aliases.isEmpty()) {
            return null;
        }
        return createTrustManagerHolder(aliases);
    }

    private TrustManagerHolder createTrustManagerHolder(Set<String> aliases) {
        var container = Arc.requireContainer();
        TlsConfigurationRegistry registry = container.select(TlsConfigurationRegistry.class).get();
        TlsConfiguration tlsConfig = tlsConfigName
                .flatMap(registry::get)
                .or(registry::getDefault)
                .orElseThrow(() -> new ConfigurationException(
                        "'" + configPrefix + ".proxy.trusted-proxy[*].truststore-alias' requires the server "
                                + "to use the TLS registry. Configure 'quarkus.tls' truststore properties."));

        TrustManagerHolder holder = new TrustManagerHolder(tlsConfig, aliases);

        HttpCertificateUpdateEventListener listener = container.select(HttpCertificateUpdateEventListener.class).get();
        listener.register(holder::onCertificateUpdate, tlsConfig.getName());

        return holder;
    }

    private Function<HttpServerRequest, TrustedProxyCheck> checkRDNs(List<String> subjectDns) {
        List<List<Rdn>> trustedDns = subjectDns.stream().map(this::toRdns).toList();
        return request -> TrustedProxyCheck.createTrustedProxyDnCheck(request, trustedDns);
    }

    private List<Rdn> toRdns(String dn) {
        try {
            var x500PrincipalName = new X500Principal(dn).getName();
            return List.copyOf(new LdapName(x500PrincipalName).getRdns());
        } catch (IllegalArgumentException | InvalidNameException e) {
            throw new ConfigurationException(
                    "Invalid '" + configPrefix + ".proxy.trusted-proxy[*].subject-dn' value '" + dn
                            + "': not a valid RFC 2253 Distinguished Name",
                    e);
        }
    }

    private ConfigurationException missingSubjectDn() {
        return new ConfigurationException(
                "Each '" + configPrefix + ".proxy.trusted-proxy' entry must have 'subject-dn' configured");
    }

    private static Function<HttpServerRequest, TrustedProxyCheck> allowAll() {
        return new Function<>() {

            private final TrustedProxyCheck allowAll = TrustedProxyCheck.allowAll();

            @Override
            public TrustedProxyCheck apply(HttpServerRequest request) {
                return allowAll;
            }
        };
    }

    private void validateTrustedProxyConfig() {
        if (!hasClientAuthTrustCheck) {

            if (hasHostTrustCheck && !allowXForwarded && !allowForwarded) {
                throw new ConfigurationException(
                        "'" + configPrefix + ".proxy.trusted-proxies' requires '" + configPrefix
                                + ".proxy.allow-forwarded' "
                                + "or '" + configPrefix + ".proxy.allow-x-forwarded' to be enabled");
            }
        } else {

            String configuredProps = trustedProxyPropertyNames();

            if (hasHostTrustCheck) {
                throw new ConfigurationException(
                        "'" + configPrefix + ".proxy.trusted-proxies' and " + configuredProps
                                + " are mutually exclusive");
            }

            if (clientAuth == ClientAuth.NONE) {
                throw new ConfigurationException(
                        configuredProps + " requires '" + configPrefix
                                + ".ssl.client-auth' to be set "
                                + "to 'request' or 'required'");
            }

            if (!allowXForwarded && !allowForwarded) {
                throw new ConfigurationException(
                        configuredProps + " requires '" + configPrefix
                                + ".proxy.allow-forwarded' "
                                + "or '" + configPrefix + ".proxy.allow-x-forwarded' to be enabled");
            }
        }
    }

    private String trustedProxyPropertyNames() {
        boolean hasAlias = proxyConfig.trustedProxy().stream().anyMatch(c -> c.truststoreAlias().isPresent());
        String prefix = "'" + configPrefix + ".proxy.trusted-proxy[*]";
        if (hasAlias) {
            return prefix + ".subject-dn' and " + prefix + ".truststore-alias'";
        }
        return prefix + ".subject-dn'";
    }

    private static Function<HttpServerRequest, TrustedProxyCheck> logicalAnd(Function<HttpServerRequest, TrustedProxyCheck> a,
            Function<HttpServerRequest, TrustedProxyCheck> b) {
        return new Function<HttpServerRequest, TrustedProxyCheck>() {
            @Override
            public TrustedProxyCheck apply(HttpServerRequest request) {
                TrustedProxyCheck first = a.apply(request);
                if (!first.isProxyAllowed()) {
                    return TrustedProxyCheck.denyAll();
                }
                return b.apply(request).isProxyAllowed() ? TrustedProxyCheck.allowAll() : TrustedProxyCheck.denyAll();
            }
        };
    }

    private static Function<HttpServerRequest, TrustedProxyCheck> logicalOr(Function<HttpServerRequest, TrustedProxyCheck> a,
            Function<HttpServerRequest, TrustedProxyCheck> b) {
        return new Function<HttpServerRequest, TrustedProxyCheck>() {
            @Override
            public TrustedProxyCheck apply(HttpServerRequest request) {
                TrustedProxyCheck first = a.apply(request);
                if (first.isProxyAllowed()) {
                    return TrustedProxyCheck.allowAll();
                }
                return b.apply(request).isProxyAllowed() ? TrustedProxyCheck.allowAll() : TrustedProxyCheck.denyAll();
            }
        };
    }

    private static final class TrustManagerHolder {

        private static final Logger LOG = Logger.getLogger(TrustManagerHolder.class);

        private final Set<String> aliases;
        private volatile Map<String, X509TrustManager> trustManagers;

        TrustManagerHolder(TlsConfiguration tlsConfig, Set<String> aliases) {
            this.aliases = aliases;
            this.trustManagers = buildTrustManagers(tlsConfig.getTrustStore(), true);
        }

        void onCertificateUpdate(TlsConfiguration tlsConfig) {
            LOG.debug("Rebuilding trusted proxy trust managers after certificate update");
            try {
                this.trustManagers = buildTrustManagers(tlsConfig.getTrustStore(), false);
            } catch (Exception e) {
                LOG.error("Failed to rebuild trusted proxy trust managers, rejecting all proxies until next successful reload",
                        e);
                this.trustManagers = Map.of();
            }
        }

        Function<HttpServerRequest, TrustedProxyCheck> forAlias(String alias) {
            return event -> {
                X509TrustManager tm = trustManagers.get(alias);
                if (tm == null) {
                    LOG.debugf("Alias '%s' no longer present in the truststore after reload, rejecting forwarded headers",
                            alias);
                    return TrustedProxyCheck.denyAll();
                }
                return TrustedProxyCheck.createTruststoreCheck(event, tm, alias);
            };
        }

        private Map<String, X509TrustManager> buildTrustManagers(KeyStore trustStore, boolean failOnMissingAlias) {
            var result = new LinkedHashMap<String, X509TrustManager>();
            for (String alias : aliases) {
                try {
                    Certificate cert = trustStore.getCertificate(alias);
                    if (cert == null) {
                        if (failOnMissingAlias) {
                            throw new ConfigurationException(
                                    "Truststore alias '" + alias + "' not found in the HTTP server truststore");
                        }
                        LOG.warnf("Truststore alias '%s' not found in the truststore after certificate reload,"
                                + " proxy certificate chain cannot be verified against this alias", alias);
                        continue;
                    }
                    KeyStore singleCertKs = KeyStore.getInstance(KeyStore.getDefaultType());
                    singleCertKs.load(null, null);
                    singleCertKs.setCertificateEntry(alias, cert);
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(singleCertKs);
                    for (TrustManager tm : tmf.getTrustManagers()) {
                        if (tm instanceof X509TrustManager x509TrustManager) {
                            result.put(alias, x509TrustManager);
                            break;
                        }
                    }
                    if (!result.containsKey(alias)) {
                        if (failOnMissingAlias) {
                            throw new IllegalStateException("No X509TrustManager produced for alias '" + alias + "'");
                        }
                        LOG.warnf("No X509TrustManager produced for alias '%s' after certificate reload,"
                                + " proxy certificate chain cannot be verified against this alias", alias);
                    }
                } catch (ConfigurationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to build trust manager for alias '" + alias + "'", e);
                }
            }
            return Map.copyOf(result);
        }
    }

    private record RequestForwardingProxyHandler(
            Function<HttpServerRequest, TrustedProxyCheck> requestToTrustedProxyCheck, Handler<HttpServerRequest> root,
            ForwardingProxyOptions forwardingProxyOptions) implements Handler<HttpServerRequest> {

        @Override
        public void handle(HttpServerRequest event) {
            ForwardedServerRequestWrapper.handleOrReject(event, forwardingProxyOptions,
                    requestToTrustedProxyCheck.apply(event), root);
        }
    }

}
