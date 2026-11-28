package io.quarkus.it.keycloak;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.runtime.AddressResolver;
import io.quarkus.security.credential.CertificateCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.tls.BaseTlsConfiguration;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.TrustOptions;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Path("/spiffe/mtls")
public class SpiffeMtlsResource {

    private static final String ANONYMOUS = "anonymous";

    @Inject
    SecurityIdentity identity;

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Inject
    Vertx vertx;

    @Inject
    @RestClient
    MtlsServerClient configuredClient;

    @GET
    @Path("/server")
    public String server() throws CertificateParsingException {
        X509Certificate certificate = identity.getCredential(CertificateCredential.class).getCertificate();
        for (List<?> name : certificate.getSubjectAlternativeNames()) {
            if (name.get(0).equals(6)) {
                return (String) name.get(1);
            }
        }
        throw new IllegalStateException("The client certificate has no URI SAN");
    }

    @GET
    @Path("/server/certificate")
    public String serverCertificate(@Context HttpServerRequest request) throws CertificateEncodingException {
        byte[] certificate = request.sslSession().getLocalCertificates()[0].getEncoded();
        return Integer.toHexString(Arrays.hashCode(certificate));
    }

    @Blocking
    @GET
    @Path("/client/rest/{tls}")
    public Response rest(@PathParam("tls") String tlsConfigurationName) throws IOException {
        MtlsServerClient client = restClient(tlsConfigurationName);
        try {
            return Response.ok(client.getSpiffeId()).build();
        } catch (RuntimeException e) {
            return failure(e);
        } finally {
            ((Closeable) client).close();
        }
    }

    @Blocking
    @GET
    @Path("/client/configured-rest")
    public String configuredRest() {
        return configuredClient.getSpiffeId();
    }

    // a new client per call, so that no TLS session is resumed and the server presents its current certificate
    @Blocking
    @GET
    @Path("/client/rest/server-certificate")
    public String restServerCertificate() throws IOException {
        MtlsServerClient client = restClient(SpiffeMtlsSetup.CLIENT);
        try {
            return client.getCertificate();
        } finally {
            ((Closeable) client).close();
        }
    }

    @GET
    @Path("/client/webclient/{tls}")
    public Uni<Response> webclient(@PathParam("tls") String tlsConfigurationName) {
        TlsConfiguration configuration = tlsConfiguration(tlsConfigurationName);
        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setKeyCertOptions(configuration.getKeyStoreOptions())
                .setTrustOptions(configuration.getTrustStoreOptions()));
        URI server = AddressResolver.resolve("quarkus://vertx-https/spiffe/mtls/server");
        return Uni.createFrom().completionStage(
                client.get(server.getPort(), server.getHost(), server.getPath()).send().toCompletionStage())
                .map(HttpResponse::bodyAsString)
                .map(spiffeId -> Response.ok(spiffeId).build())
                .onFailure().recoverWithItem(SpiffeMtlsResource::failure)
                .eventually(client::close);
    }

    private MtlsServerClient restClient(String tlsConfigurationName) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(AddressResolver.resolve("quarkus://vertx-https"))
                .tlsConfiguration(tlsConfiguration(tlsConfigurationName))
                .build(MtlsServerClient.class);
    }

    private TlsConfiguration tlsConfiguration(String tlsConfigurationName) {
        if (!ANONYMOUS.equals(tlsConfigurationName)) {
            return tlsRegistry.get(tlsConfigurationName).orElseThrow();
        }
        // trusts the server, but presents no client certificate
        TrustOptions trustOptions = tlsRegistry.get(SpiffeMtlsSetup.CLIENT).orElseThrow().getTrustStoreOptions();
        return new BaseTlsConfiguration() {
            @Override
            public TrustOptions getTrustStoreOptions() {
                return trustOptions;
            }
        };
    }

    private static Response failure(Throwable failure) {
        StringBuilder causes = new StringBuilder();
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            causes.append(cause).append('\n');
        }
        return Response.serverError().entity(causes.toString()).build();
    }
}
