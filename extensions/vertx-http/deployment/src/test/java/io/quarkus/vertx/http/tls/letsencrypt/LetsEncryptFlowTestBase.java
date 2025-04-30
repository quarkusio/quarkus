package io.quarkus.vertx.http.tls.letsencrypt;

import static io.quarkus.vertx.http.runtime.RouteConstants.ROUTE_ORDER_BODY_HANDLER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.net.ssl.SSLHandshakeException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;

public abstract class LetsEncryptFlowTestBase {

    static final File SELF_SIGNED_CERT = new File("target/certs/lets-encrypt/self-signed.crt");
    static final File SELF_SIGNED_KEY = new File("target/certs/lets-encrypt/self-signed.key");
    static final File SELF_SIGNED_CA = new File("target/certs/lets-encrypt/self-signed-ca.crt");

    static final File ACME_CERT = new File("target/certs/lets-encrypt/acme.crt");
    static final File ACME_KEY = new File("target/certs/lets-encrypt/acme.key");
    static final File ACME_CA = new File("target/certs/lets-encrypt/acme-ca.crt");

    private Vertx vertx;
    private String tlsConfigurationName;

    static <T> T await(Future<T> future) {
        return future.toCompletionStage().toCompletableFuture().join();
    }

    public void initFlow(Vertx vertx, String tlsConfigurationName) {
        this.vertx = vertx;
        this.tlsConfigurationName = tlsConfigurationName;
    }

    abstract void updateCerts() throws IOException;

    abstract String getApplicationEndpoint();

    abstract String getLetsEncryptManagementEndpoint();

    abstract String getLetsEncryptCertsEndpoint();

    abstract String getChallengeEndpoint();

    void testLetsEncryptFlow() throws IOException {
        WebClientOptions options = new WebClientOptions().setSsl(true)
                .setTrustOptions(new PemTrustOptions().addCertPath(SELF_SIGNED_CA.getAbsolutePath()));
        WebClient client = WebClient.create(vertx, options);

        String readyEndpoint = getLetsEncryptManagementEndpoint();
        if (tlsConfigurationName != null) {
            readyEndpoint = readyEndpoint + "?key=" + tlsConfigurationName;
        }

        String reloadEndpoint = getLetsEncryptCertsEndpoint();
        if (tlsConfigurationName != null) {
            reloadEndpoint = reloadEndpoint + "?key=" + tlsConfigurationName;
        }

        //  Verify the application is serving the application
        HttpResponse<Buffer> response = await(client.getAbs(getApplicationEndpoint()).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        String body = response.bodyAsString(); // We will need it later.

        // Verify if the application is ready to serve the challenge
        response = await(client.getAbs(readyEndpoint).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(204);

        // Make sure invalid tokens are rejected
        response = await(client.postAbs(getLetsEncryptManagementEndpoint()).sendJsonObject(
                new JsonObject()));
        Assertions.assertThat(response.statusCode()).isEqualTo(400);

        response = await(client.postAbs(getLetsEncryptManagementEndpoint()).sendJsonObject(
                new JsonObject()
                        .put("challenge-content", "aaa")));
        Assertions.assertThat(response.statusCode()).isEqualTo(400);

        response = await(client.postAbs(getLetsEncryptManagementEndpoint()).sendJsonObject(
                new JsonObject()
                        .put("challenge-resource", "aaa")));
        Assertions.assertThat(response.statusCode()).isEqualTo(400);

        // Set the challenge
        String challengeContent = UUID.randomUUID().toString();
        String challengeToken = UUID.randomUUID().toString();
        response = await(client.postAbs(getLetsEncryptManagementEndpoint()).sendJsonObject(
                new JsonObject()
                        .put("challenge-content", challengeContent)
                        .put("challenge-resource", challengeToken)));

        Assertions.assertThat(response.statusCode()).isEqualTo(204);

        // Verify that the challenge is set
        response = await(client.getAbs(readyEndpoint).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.bodyAsJsonObject()).isEqualTo(new JsonObject()
                .put("challenge-content", challengeContent)
                .put("challenge-resource", challengeToken));

        // Make sure the challenge cannot be set again
        response = await(client.postAbs(getLetsEncryptManagementEndpoint()).sendJsonObject(
                new JsonObject().put("challenge-resource", "again").put("challenge-content", "again")));
        Assertions.assertThat(response.statusCode()).isEqualTo(400);

        // Verify that the let's encrypt management endpoint only support GET, POST and DELETE
        // Make sure the challenge cannot be set again
        response = await(client.patchAbs(getLetsEncryptManagementEndpoint()).sendBuffer(Buffer.buffer("again")));
        Assertions.assertThat(response.statusCode()).isEqualTo(405);

        // Verify that the application is serving the challenge
        response = await(client.getAbs(getChallengeEndpoint() + "/" + challengeToken).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.bodyAsString()).isEqualTo(challengeContent);

        // Verify that other path and token are not valid
        response = await(client.getAbs(getChallengeEndpoint() + "/" + "whatever").send());
        Assertions.assertThat(response.statusCode()).isEqualTo(404);
        response = await(client.getAbs(getChallengeEndpoint()).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(404);

        // Verify that only GET is supported when serving the challenge
        response = await(client.postAbs(getChallengeEndpoint()).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(405);
        response = await(client.deleteAbs(getChallengeEndpoint()).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(405);
        response = await(client.postAbs(getChallengeEndpoint() + "/" + challengeToken).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(405);
        response = await(client.deleteAbs(getChallengeEndpoint() + "/" + challengeToken).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(405);

        // Verify that the challenge can be served multiple times
        response = await(client.getAbs(getChallengeEndpoint() + "/" + challengeToken).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.bodyAsString()).isEqualTo(challengeContent);

        // Replace the certs on disk
        updateCerts();

        // Clear the challenge
        response = await(client.deleteAbs(getLetsEncryptManagementEndpoint()).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(204);

        // Check we cannot clear the challenge again
        response = await(client.deleteAbs(getLetsEncryptManagementEndpoint()).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(404);

        // Check we cannot retrieve the challenge again
        response = await(client.getAbs(getChallengeEndpoint()).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(404);

        // Reload the certificate
        response = await(client.postAbs(reloadEndpoint).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(204);

        // Verify that reload cannot be call with other verb
        response = await(client.getAbs(reloadEndpoint).send());
        Assertions.assertThat(response.statusCode()).isEqualTo(405);

        // Verify the application is serving the new certificate
        // We should not use the WebClient as the connection are still established with the old certificate.
        URL url = new URL(getApplicationEndpoint());
        assertThatThrownBy(() -> vertx.createHttpClient(
                new HttpClientOptions().setSsl(true).setDefaultPort(url.getPort())
                        .setTrustOptions(new PemTrustOptions().addCertPath(SELF_SIGNED_CA.getAbsolutePath())))
                .request(HttpMethod.GET, "/tls")
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join()).hasCauseInstanceOf(SSLHandshakeException.class);

        WebClient newWebClient = WebClient.create(vertx,
                options.setTrustOptions(new PemTrustOptions().addCertPath(ACME_CA.getAbsolutePath())));
        String newBody = await(newWebClient.getAbs(url.toString()).send()).bodyAsString();
        Assertions.assertThat(newBody).isNotEqualTo(body);
    }

    @ApplicationScoped
    public static class MyBean {

        public void register(@Observes Router router) {
            router.route().order(ROUTE_ORDER_BODY_HANDLER).handler(BodyHandler.create());
            router
                    .get("/tls").handler(rc -> {
                        Assertions.assertThat(rc.request().connection().isSsl()).isTrue();
                        Assertions.assertThat(rc.request().isSSL()).isTrue();
                        Assertions.assertThat(rc.request().connection().sslSession()).isNotNull();
                        var exp = ((X509Certificate) rc.request().connection().sslSession().getLocalCertificates()[0])
                                .getNotAfter().toInstant().toEpochMilli();
                        rc.response().end("expiration: " + exp);
                    });
        }
    }

}
