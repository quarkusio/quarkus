package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class CertificateRoleMappingTest {

    @TestHTTPResource(ssl = true)
    URL url;

    @Test
    public void testAuthenticated() {
        given().spec(getMtlsRequestSpec("client-keystore-1.jks")).get("/protected/authenticated")
                .then().body(equalTo("CN=client,OU=cert,O=quarkus,L=city,ST=state,C=AU"));
        given().spec(getMtlsRequestSpec("client-keystore-2.jks")).get("/protected/authenticated")
                .then().body(equalTo("CN=localhost,OU=quarkus,O=quarkus,L=city,ST=state,C=IE"));
    }

    @Test
    public void testAuthorizedUser() {
        given().spec(getMtlsRequestSpec("client-keystore-1.jks")).get("/protected/authorized-user")
                .then().body(equalTo("CN=client,OU=cert,O=quarkus,L=city,ST=state,C=AU"));
        given().spec(getMtlsRequestSpec("client-keystore-2.jks")).get("/protected/authorized-user")
                .then().body(equalTo("CN=localhost,OU=quarkus,O=quarkus,L=city,ST=state,C=IE"));
    }

    @Test
    public void testAuthorizedAdmin() {
        given().spec(getMtlsRequestSpec("client-keystore-1.jks")).get("/protected/authorized-admin")
                .then().body(equalTo("CN=client,OU=cert,O=quarkus,L=city,ST=state,C=AU"));
        given().spec(getMtlsRequestSpec("client-keystore-2.jks")).get("/protected/authorized-admin")
                .then().statusCode(403);
    }

    @Test
    public void testNoClientCertificate() {
        given().get("/protected/authenticated").then().statusCode(401);
        given().get("/protected/authorized-user").then().statusCode(401);
        given().get("/protected/authorized-admin").then().statusCode(401);
    }

    private RequestSpecification getMtlsRequestSpec(String clientKeyStore) {
        return new RequestSpecBuilder()
                .setBaseUri(String.format("%s://%s", url.getProtocol(), url.getHost()))
                .setPort(url.getPort())
                .setKeyStore(clientKeyStore, "password")
                .setTrustStore("client-truststore.jks", "password")
                .build();
    }
}
