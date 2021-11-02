package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultBootstrapConfig.KUBERNETES_CACERT;

import org.jboss.logging.Logger;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class MutinyVertxClientFactory {

    private static final Logger log = Logger.getLogger(MutinyVertxClientFactory.class.getName());

    public static WebClient createHttpClient(Vertx vertx, VaultBootstrapConfig vaultBootstrapConfig, TlsConfig tlsConfig) {

        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout((int) vaultBootstrapConfig.connectTimeout.toMillis())
                .setIdleTimeout((int) vaultBootstrapConfig.readTimeout.getSeconds() * 2);

        if (vaultBootstrapConfig.nonProxyHosts.isPresent()) {
            options.setNonProxyHosts(vaultBootstrapConfig.nonProxyHosts.get());
        }

        boolean trustAll = vaultBootstrapConfig.tls.skipVerify.orElseGet(() -> tlsConfig.trustAll);
        if (trustAll) {
            skipVerify(options);
        } else if (vaultBootstrapConfig.tls.caCert.isPresent()) {
            cacert(options, vaultBootstrapConfig.tls.caCert.get());
        } else if (vaultBootstrapConfig.getAuthenticationType() == KUBERNETES
                && vaultBootstrapConfig.tls.useKubernetesCaCert) {
            cacert(options, KUBERNETES_CACERT);
        }

        return WebClient.create(vertx, options);
    }

    private static void cacert(WebClientOptions options, String cacert) {
        log.debug("configure tls with " + cacert);
        options.setTrustOptions(new PemTrustOptions().addCertPath(cacert));
    }

    private static void skipVerify(WebClientOptions options) {
        log.debug("configure tls with skip-verify");
        options.setTrustAll(true);
        options.setVerifyHost(false);
    }
}
