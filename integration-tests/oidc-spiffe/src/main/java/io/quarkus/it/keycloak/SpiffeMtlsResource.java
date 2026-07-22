package io.quarkus.it.keycloak;

import java.net.URI;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.spiffe.client.SpiffeClient;
import io.quarkus.tls.BaseTlsConfiguration;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Path("/spiffe/mtls")
public class SpiffeMtlsResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Inject
    SpiffeClient spiffeClient;

    @Inject
    Vertx vertx;

    public record MtlsResult(String actualPrincipal, String expectedPrincipal) {
    }

    @GET
    @Path("/server")
    public String server() {
        return identity.getPrincipal().getName();
    }

    @GET
    @Path("/client/webclient/authenticated")
    public Uni<MtlsResult> webclientAuthenticated(@QueryParam("port") int port) {
        return spiffeClient.getWorkloadCertificate()
                .flatMap(cert -> {
                    var options = new WebClientOptions()
                            .setSsl(true)
                            .setVerifyHost(false)
                            .setKeyCertOptions(cert.keyMaterial().asVertxKeyCertOptions())
                            .setTrustOptions(cert.trustMaterial().asVertxTrustOptions());
                    WebClient client = WebClient.create(vertx, options);

                    String expectedPrincipal = cert.keyMaterial().certificateChain().get(0)
                            .getSubjectX500Principal().getName();
                    return Uni.createFrom().completionStage(
                            client.get(port, "localhost", "/spiffe/mtls/server")
                                    .send()
                                    .toCompletionStage())
                            .map(HttpResponse::bodyAsString)
                            .map(actual -> new MtlsResult(actual, expectedPrincipal))
                            .eventually(client::close);
                });
    }

    @GET
    @Path("/client/webclient/unauthorized")
    public Uni<String> webclientUnauthorized(@QueryParam("port") int port) {
        TlsConfiguration tls = tlsRegistry.get("spiffe-mtls").orElseThrow();
        var options = new WebClientOptions()
                .setSsl(true)
                .setVerifyHost(false)
                .setTrustOptions(tls.getTrustStoreOptions());
        WebClient client = WebClient.create(vertx, options);
        return Uni.createFrom().completionStage(
                client.get(port, "localhost", "/spiffe/mtls/server").send().toCompletionStage())
                .map(r -> String.valueOf(r.statusCode()))
                .eventually(client::close);
    }

    @Blocking
    @GET
    @Path("/client/rest/authenticated")
    public MtlsResult restAuthenticated(@QueryParam("port") int port) {
        var cert = spiffeClient.getWorkloadCertificate().await().indefinitely();
        String expectedPrincipal = cert.keyMaterial().certificateChain().get(0)
                .getSubjectX500Principal().getName();
        TlsConfiguration tlsConfiguration = new SpiffeTlsConfiguration(cert.keyMaterial().asVertxKeyCertOptions(),
                cert.trustMaterial().asVertxTrustOptions());

        MtlsServerClient client = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://localhost:" + port))
                .tlsConfiguration(tlsConfiguration)
                .build(MtlsServerClient.class);
        String actual = client.getPrincipal();
        return new MtlsResult(actual, expectedPrincipal);
    }

    @Blocking
    @GET
    @Path("/client/rest/unauthorized")
    public String restUnauthorized(@QueryParam("port") int port) {
        var cert = spiffeClient.getWorkloadCertificate().await().indefinitely();
        TlsConfiguration trustOnly = new SpiffeTlsConfiguration(null, cert.trustMaterial().asVertxTrustOptions());

        MtlsServerClient client = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://localhost:" + port))
                .tlsConfiguration(trustOnly)
                .build(MtlsServerClient.class);
        try {
            client.getPrincipal();
            return "200";
        } catch (WebApplicationException e) {
            return String.valueOf(e.getResponse().getStatus());
        }
    }

    private static final class SpiffeTlsConfiguration extends BaseTlsConfiguration {

        private final KeyCertOptions keyCertOptions;
        private final TrustOptions trustOptions;

        private SpiffeTlsConfiguration(KeyCertOptions keyCertOptions, TrustOptions trustOptions) {
            this.keyCertOptions = keyCertOptions;
            this.trustOptions = trustOptions;
        }

        @Override
        public KeyCertOptions getKeyStoreOptions() {
            return keyCertOptions;
        }

        @Override
        public TrustOptions getTrustStoreOptions() {
            return trustOptions;
        }

        @Override
        public Optional<String> getHostnameVerificationAlgorithm() {
            return Optional.of("NONE");
        }
    }
}
