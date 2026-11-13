package io.quarkus.it.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.kafka.ssl.CertificateFormat;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(certificates = {
        @Certificate(name = "kafka", formats = { Format.PKCS12, Format.JKS,
                Format.PEM }, password = "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L")
}, baseDir = "target/certs")
@QuarkusTest
@QuarkusTestResource(value = KafkaSSLTestResource.class, initArgs = {
        @ResourceArg(name = "kafka.tls-configuration-name", value = "custom-p12")
}, restrictToAnnotatedClass = true)
public class SslPKCS12KafkaConsumerTest extends SslKafkaConsumerTest {

    @Override
    public CertificateFormat getFormat() {
        return CertificateFormat.PKCS12;
    }

    @Test
    void certificateReloadDoesNotBreakConnections() {
        int reloadCount = RestAssured.given()
                .queryParam("name", "custom-p12")
                .post("/ssl/certificate-reload")
                .then()
                .statusCode(200)
                .extract().as(Integer.class);
        Assertions.assertTrue(reloadCount > 0, "Reload event should have been observed");

        try (Producer<Integer, String> producer = createProducer(CertificateFormat.PKCS12)) {
            producer.send(new ProducerRecord<>("test-ssl-consumer", 1, "after reload"));
        }
        String value = RestAssured.given()
                .queryParam("format", "PKCS12")
                .get("/ssl")
                .asString();
        Assertions.assertEquals("after reload", value);
    }
}
