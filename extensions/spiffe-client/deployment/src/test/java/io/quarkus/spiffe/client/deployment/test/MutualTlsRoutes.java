package io.quarkus.spiffe.client.deployment.test;

import java.net.URI;
import java.security.cert.X509Certificate;

import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.AddressResolver;
import io.quarkus.spiffe.svid.x509.X509Svid;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;

@X509Svid(tls = "spiffe-server")
@X509Svid(tls = "spiffe-client", spiffeId = "spiffe://test.quarkus.io/test-workload")
class MutualTlsRoutes {

    void routes(@Observes Router router, TlsConfigurationRegistry registry, Vertx vertx) {
        router.get("/peer").handler(rc -> {
            try {
                X509Certificate client = (X509Certificate) rc.request().connection().peerCertificates().getFirst();
                rc.end("before " + client.getSubjectAlternativeNames().iterator().next().get(1));
            } catch (Exception e) {
                rc.fail(e);
            }
        });
        router.get("/call").handler(rc -> {
            TlsConfiguration configuration = registry.get("spiffe-client").orElseThrow();
            HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                    .setSsl(true)
                    .setKeyCertOptions(configuration.getKeyStoreOptions())
                    .setTrustOptions(configuration.getTrustStoreOptions()));
            URI peer = AddressResolver.resolve("quarkus://vertx-https/peer");
            client.request(HttpMethod.GET, peer.getPort(), peer.getHost(), peer.getPath())
                    .compose(request -> request.send())
                    .compose(HttpClientResponse::body)
                    .andThen(ignored -> client.close())
                    .onSuccess(body -> rc.end(body))
                    .onFailure(failure -> rc.response().setStatusCode(500).end(failure.toString()));
        });
    }
}
