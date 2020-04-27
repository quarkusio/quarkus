package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.client.CertificateHelper.createSslContext;
import static io.quarkus.vault.runtime.client.CertificateHelper.createTrustManagers;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.KUBERNETES_CACERT;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class MutinyVertxClientFactory {

    private static final Logger log = Logger.getLogger(MutinyVertxClientFactory.class.getName());

    public static WebClient createHttpClient(VaultRuntimeConfig serverConfig) {

        Vertx vertx = Arc.container().instance(Vertx.class).get();
        WebClientOptions options = new WebClientOptions();
        options.setConnectTimeout((int) serverConfig.connectTimeout.getSeconds());
        options.setIdleTimeout((int) serverConfig.readTimeout.getSeconds());
        //OkHttpClient.Builder builder = new OkHttpClient.Builder()
        //        .connectTimeout(serverConfig.connectTimeout)
        //        .readTimeout(serverConfig.readTimeout);

        try {
            if (serverConfig.tls.skipVerify) {
                skipVerify(options);
            } else if (serverConfig.tls.caCert.isPresent()) {
                cacert(options, serverConfig.tls.caCert.get());
            } else if (serverConfig.getAuthenticationType() == KUBERNETES && serverConfig.tls.useKubernetesCaCert) {
                cacert(options, KUBERNETES_CACERT);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new VaultException(e);
        }

        return WebClient.create(vertx, options);
    }

    private static void cacert(WebClientOptions options, String cacert) throws GeneralSecurityException, IOException {
        log.debug("create SSLSocketFactory with tls " + cacert);
        sslSocketFactory(options, createTrustManagers(cacert));
    }

    private static void skipVerify(WebClientOptions options) throws GeneralSecurityException {
        log.debug("create SSLSocketFactory with tls.skip-verify");
        options.setTrustAll(true);
        options.setVerifyHost(false);
    }

    private static void sslSocketFactory(WebClientOptions options, TrustManager[] trustManagers)
            throws GeneralSecurityException {
        SSLContext sslContext = createSslContext(trustManagers);
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        //builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0]);
    }
}
