package io.quarkus.oidc.common.runtime;

import java.io.Closeable;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.common.runtime.config.OidcCommonConfig;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public final class OidcWebClient implements Closeable {

    private final WebClient webClient;
    private final Runnable onClose;

    private OidcWebClient(WebClient webClient, Runnable onClose) {
        this.webClient = webClient;
        this.onClose = onClose;
    }

    public HttpRequest<Buffer> getAbs(String absoluteURI) {
        return webClient.getAbs(absoluteURI);
    }

    public HttpRequest<Buffer> postAbs(String absoluteURI) {
        return webClient.postAbs(absoluteURI);
    }

    public HttpRequest<Buffer> headAbs(String absoluteURI) {
        return webClient.headAbs(absoluteURI);
    }

    public HttpRequest<Buffer> putAbs(String absoluteURI) {
        return webClient.putAbs(absoluteURI);
    }

    public HttpRequest<Buffer> deleteAbs(String absoluteURI) {
        return webClient.deleteAbs(absoluteURI);
    }

    @Override
    public void close() {
        if (onClose != null) {
            onClose.run();
        }
        webClient.close();
    }

    public static OidcWebClient create(OidcCommonConfig oidcConfig, OidcTlsSupport tlsSupport, Vertx vertx,
            ProxyConfigurationRegistry proxyConfigurationRegistry, String clientUser) {
        var tlsConfigSupport = tlsSupport.forConfig(oidcConfig.tls());
        var webClient = createWebClient(oidcConfig, tlsConfigSupport, vertx, proxyConfigurationRegistry);
        final Runnable onClose;
        if (tlsConfigSupport.useTlsRegistryAndMtls()) {
            onClose = Arc.requireContainer().select(CertificateUpdateEventListener.class).get()
                    .registerWebClient(tlsConfigSupport.getTlsConfigName(), webClient, clientUser);
        } else {
            onClose = null;
        }
        return new OidcWebClient(webClient, onClose);
    }

    private static WebClient createWebClient(OidcCommonConfig oidcConfig, OidcTlsSupport.TlsConfigSupport tlsConfigSupport,
            Vertx vertx, ProxyConfigurationRegistry proxyConfigurationRegistry) {
        WebClientOptions options = new WebClientOptions().setFollowRedirects(oidcConfig.followRedirects());
        OidcCommonUtils.setHttpClientOptions(oidcConfig, options, tlsConfigSupport, proxyConfigurationRegistry);
        return WebClient.create(vertx, options);
    }
}
