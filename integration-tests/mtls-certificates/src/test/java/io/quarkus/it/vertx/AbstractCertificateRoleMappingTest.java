package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

public abstract class AbstractCertificateRoleMappingTest {

    @TestHTTPResource(ssl = true)
    URL url;

    @Test
    public void testAuthenticated() {
        given().spec(getMtlsRequestSpec("client-keystore-1.p12")).get("/protected/authenticated")
                .then().body(equalTo(getClient1Dn()));
        given().spec(getMtlsRequestSpec("client-keystore-2.p12")).get("/protected/authenticated")
                .then().body(equalTo(getClient2Dn()));
    }

    @Test
    public void testAuthorizedUser() {
        given().spec(getMtlsRequestSpec("client-keystore-1.p12")).get("/protected/authorized-user")
                .then().body(equalTo(getClient1Dn()));
        given().spec(getMtlsRequestSpec("client-keystore-2.p12")).get("/protected/authorized-user")
                .then().body(equalTo(getClient2Dn()));
    }

    @Test
    public void testAuthorizedAdmin() {
        given().spec(getMtlsRequestSpec("client-keystore-1.p12")).get("/protected/authorized-admin")
                .then().body(equalTo(getClient1Dn()));
        given().spec(getMtlsRequestSpec("client-keystore-2.p12")).get("/protected/authorized-admin")
                .then().statusCode(403);
    }

    @Test
    public void testNoClientCertificate() {
        // javax.net.ssl.SSLHandshakeException
        // Indicates that the client and server could not negotiate the desired level of security.
        // The connection is no longer usable.
        final RequestSpecification rs = new RequestSpecBuilder()
                .setBaseUri(String.format("%s://%s", url.getProtocol(), url.getHost()))
                .setPort(url.getPort()).build();
        assertThrows(SSLHandshakeException.class,
                () -> given().spec(rs).get("/protected/authenticated"),
                "Insecure requests must fail at the transport level");
        assertThrows(SSLHandshakeException.class,
                () -> given().spec(rs).get("/protected/authorized-user"),
                "Insecure requests must fail at the transport level");
        assertThrows(SSLHandshakeException.class,
                () -> given().spec(rs).get("/protected/authorized-admin"),
                "Insecure requests must fail at the transport level");
    }

    protected RequestSpecification getMtlsRequestSpec(String clientKeyStore) {
        final RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(String.format("%s://%s", url.getProtocol(), url.getHost()))
                .setPort(url.getPort());
        withKeyStore(builder, clientKeyStore);
        withTrustStore(builder);
        return builder.build();
    }

    protected void withKeyStore(RequestSpecBuilder requestSpecBuilder, String clientKeyStore) {
        requestSpecBuilder.setKeyStore(clientKeyStore, "password");
    }

    protected void withTrustStore(RequestSpecBuilder requestSpecBuilder) {
        requestSpecBuilder.setTrustStore("client-truststore.p12", "password");
    }

    protected String getClient1Dn() {
        return "C=AU,ST=state,L=city,O=quarkus,OU=cert,CN=client";
    }

    protected String getClient2Dn() {
        return "C=IE,ST=state,L=city,O=quarkus,OU=quarkus,CN=localhost";
    }
}
