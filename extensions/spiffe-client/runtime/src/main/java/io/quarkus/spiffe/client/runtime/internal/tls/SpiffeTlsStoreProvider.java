package io.quarkus.spiffe.client.runtime.internal.tls;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.spiffe.client.SpiffeConnectionException;
import io.quarkus.spiffe.client.runtime.internal.SpiffeValidator;
import io.quarkus.spiffe.client.runtime.internal.tls.X509SvidCachingClient.X509SvidCachingClientImpl;
import io.quarkus.spiffe.svid.x509.WorkloadCertificateDocument;
import io.quarkus.spiffe.svid.x509.X509Svid;
import io.quarkus.spiffe.svid.x509.X509SvidClient;
import io.quarkus.tls.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.KeyStoreProvider;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.TrustStoreAndTrustOptions;
import io.quarkus.tls.TrustStoreProvider;
import io.smallrye.common.expression.Expression;
import io.smallrye.config.Config;
import io.vertx.core.Vertx;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.ServerSSLOptions;

public class SpiffeTlsStoreProvider implements KeyStoreProvider, TrustStoreProvider {

    // published by the SPIFFE Dev Service, which does not issue X.509-SVIDs
    public static final String DEV_SERVICE_ENDPOINT_SOCKET = "dev-svc.quarkus.spiffe-client.devservices.endpoint-socket";
    private static final Logger LOG = Logger.getLogger(SpiffeTlsStoreProvider.class);
    // every start replaces it, and the managers read it on every handshake because a dev mode restart keeps the HTTP
    // server with the managers of the first start
    private static volatile X509SvidCachingClient cachingClient;

    private final SpiffeKeyManager keyManager;
    private final SpiffeTrustManager trustManager;

    protected SpiffeTlsStoreProvider(String tlsConfigurationName, String spiffeId) {
        this.keyManager = new SpiffeKeyManager(SpiffeTlsStoreProvider::currentWorkloadCertificate, tlsConfigurationName);
        this.trustManager = new SpiffeTrustManager(SpiffeTlsStoreProvider::currentWorkloadCertificate, tlsConfigurationName,
                resolveSpiffeId(tlsConfigurationName, spiffeId));
    }

    // the service started after the TLS registry: fetches the first X.509-SVID, keeps it refreshed, stops at shutdown
    public static void start(Vertx vertx, Duration refreshPeriod, Consumer<Runnable> onStop,
            Consumer<WorkloadCertificateDocument> onComplete, Consumer<Throwable> onFailure, X509SvidClient x509SvidClient,
            TlsConfigurationRegistry registry, Set<String> tlsConfigurationNames) {
        for (String tlsConfigurationName : tlsConfigurationNames) {
            TlsConfiguration configuration = registry.get(tlsConfigurationName).orElseThrow();
            // Vert.x wraps a trust manager with a CRL into a plain X509TrustManager, the JDK then verifies host names
            ServerSSLOptions options = configuration.getServerSSLOptions();
            if (!options.getCrlValues().isEmpty() || !options.getCrlPaths().isEmpty()) {
                throw new ConfigurationException("The TLS configuration '" + tlsConfigurationName
                        + "' is backed by the X.509-SVID and does not support a certificate revocation list",
                        Set.of("quarkus.tls." + tlsConfigurationName + ".certificate-revocation-list"));
            }
            if (configuration.getSslEngineOptions().orElse(null) instanceof OpenSSLEngineOptions) {
                throw new ConfigurationException("The TLS configuration '" + tlsConfigurationName
                        + "' is backed by the X.509-SVID and does not support the OpenSSL engine",
                        Set.of("quarkus.tls." + tlsConfigurationName + ".ssl-engine"));
            }
        }
        if (isDevServiceEndpoint()) {
            LOG.warnf("The SPIFFE Dev Service does not issue X.509-SVIDs, so TLS handshakes with the configurations %s"
                    + " declared with @X509Svid fail. Configure 'quarkus.spiffe-client.endpoint-socket' with a Workload API"
                    + " that issues X.509-SVIDs.", tlsConfigurationNames);
            onComplete.accept(null);
            return;
        }
        X509SvidCachingClientImpl client = new X509SvidCachingClientImpl(x509SvidClient);
        cachingClient = client;
        client.initialize(vertx, refreshPeriod, onStop, onFailure, onComplete);
    }

    private static boolean isDevServiceEndpoint() {
        Config config = Config.get();
        Optional<String> devServiceEndpoint = config.getOptionalValue(DEV_SERVICE_ENDPOINT_SOCKET, String.class);
        return devServiceEndpoint.isPresent()
                && devServiceEndpoint.equals(config.getOptionalValue("quarkus.spiffe-client.endpoint-socket", String.class));
    }

    private static WorkloadCertificateDocument currentWorkloadCertificate() {
        X509SvidCachingClient client = cachingClient;
        return client == null ? null : client.getCurrentWorkloadCertificate();
    }

    @Override
    public KeyStoreAndKeyCertOptions getKeyStore(Vertx vertx) {
        // no raw store on purpose, the key manager reads the current X.509-SVID on every handshake
        return new KeyStoreAndKeyCertOptions(null, keyManager);
    }

    @Override
    public TrustStoreAndTrustOptions getTrustStore(Vertx vertx) {
        // no raw store on purpose, a raw trust store cannot enforce the SPIFFE ID authorization
        return new TrustStoreAndTrustOptions(null, trustManager);
    }

    private static String resolveSpiffeId(String tlsConfigurationName, String spiffeId) {
        if (spiffeId == null || !spiffeId.contains("${")) {
            return spiffeId;
        }
        Config config = Config.get();
        String resolved = Expression.compile(spiffeId, LENIENT_SYNTAX, NO_TRIM).evaluate((context, builder) -> {
            Optional<String> value = config.getOptionalValue(context.getKey(), String.class);
            if (value.isPresent()) {
                builder.append(value.get());
            } else if (context.hasDefault()) {
                context.expandDefault();
            } else {
                throw new ConfigurationException("Could not expand '" + context.getKey() + "' in the SPIFFE ID '"
                        + spiffeId + "' of the TLS configuration '" + tlsConfigurationName + "'",
                        Set.of(context.getKey()));
            }
        });
        LOG.debugf("The SPIFFE ID '%s' of the TLS configuration '%s' resolved to '%s'", spiffeId, tlsConfigurationName,
                resolved);
        // literals were validated by the SPIFFE client processor at build time, an expanded value only exists now
        if (!X509Svid.SAME_TRUST_DOMAIN.equals(resolved)) {
            try {
                SpiffeValidator.validateSpiffeId(resolved);
            } catch (SpiffeConnectionException e) {
                throw new ConfigurationException("Invalid peer SPIFFE ID '" + resolved + "' of the TLS configuration '"
                        + tlsConfigurationName + "': " + e.getMessage(), e);
            }
        }
        return resolved;
    }
}
