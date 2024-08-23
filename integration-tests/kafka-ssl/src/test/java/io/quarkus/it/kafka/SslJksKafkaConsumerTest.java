package io.quarkus.it.kafka;

import io.quarkus.it.kafka.ssl.CertificateFormat;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(certificates = {
        @Certificate(name = "kafka", formats = { Format.PKCS12, Format.JKS,
                Format.PEM }, password = "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L")
}, baseDir = "target/certs")
@QuarkusTest
@WithTestResource(value = KafkaSSLTestResource.class, initArgs = {
        @ResourceArg(name = "kafka.tls-configuration-name", value = "custom-jks")
})
public class SslJksKafkaConsumerTest extends SslKafkaConsumerTest {

    @Override
    public CertificateFormat getFormat() {
        return CertificateFormat.JKS;
    }
}
