package io.quarkus.it.keycloak;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
@QuarkusTestResource(value = SpiffeX509MtlsTestResource.class, restrictToAnnotatedClass = true)
class SpiffeMtlsTest {

    @ParameterizedTest
    @ValueSource(strings = { "rest", "webclient" })
    void serverAuthenticatesClientSpiffeId(String client) {
        call(client, "spiffe-client").statusCode(200).body(equalTo("spiffe://test.quarkus.io/test-workload"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest", "webclient" })
    void clientRejectsServerWithOtherSpiffeId(String client) {
        call(client, "spiffe-other-peer").statusCode(500)
                .body(containsString("is not authorized by the TLS configuration 'spiffe-other-peer'"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest", "webclient" })
    void serverRejectsClientWithoutCertificate(String client) {
        call(client, "anonymous").statusCode(500);
    }

    @Test
    void configuredRestClientUsesTlsConfigurationByName() {
        RestAssured.get("/spiffe/mtls/client/configured-rest")
                .then().statusCode(200).body(equalTo("spiffe://test.quarkus.io/test-workload"));
    }

    @Test
    void serverPresentsRotatedSvid() {
        String first = serverCertificate();
        await().atMost(Duration.ofSeconds(10)).until(() -> !serverCertificate().equals(first));
    }

    private ValidatableResponse call(String client, String tlsConfigurationName) {
        return RestAssured.get("/spiffe/mtls/client/" + client + "/" + tlsConfigurationName).then();
    }

    private String serverCertificate() {
        return RestAssured.get("/spiffe/mtls/client/rest/server-certificate").then().statusCode(200).extract().asString();
    }
}
