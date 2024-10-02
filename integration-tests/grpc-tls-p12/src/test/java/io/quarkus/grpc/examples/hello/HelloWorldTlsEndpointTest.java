package io.quarkus.grpc.examples.hello;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "grpc-tls", password = "wibble", formats = {
        Format.PKCS12, Format.PEM }))
@QuarkusTest
class HelloWorldTlsEndpointTest {

    @BeforeEach
    void init() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @AfterEach
    void reset() {
        RestAssured.reset();
    }

    @Test
    public void testHelloWorldServiceUsingBlockingStub() {
        String response = get("/hello/blocking/neo").asString();
        assertThat(response).isEqualTo("Hello neo");
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() {
        String response = get("/hello/mutiny/neo-mutiny").asString();
        assertThat(response).isEqualTo("Hello neo-mutiny");
    }

}
