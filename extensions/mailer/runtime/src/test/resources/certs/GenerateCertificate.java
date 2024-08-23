///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.certs:smallrye-certificate-generator:0.8.1

import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;

import java.io.File;
import java.time.Duration;

public class GenerateCertificate {

    public static void main(String[] args) throws Exception {
        CertificateGenerator generator = new CertificateGenerator(new File(".").toPath(), true);

        generator.generate(new CertificateRequest()
                .withFormat(Format.JKS)
                .withPassword("password")
                .withSubjectAlternativeName("DNS:localhost")
                .withName("quarkus-mailer-test")
                .withDuration(Duration.ofDays(1095))
        );

        new File("quarkus-mailer-test-keystore.jks").renameTo(new File("keystore.jks"));
        new File("quarkus-mailer-test-truststore.jks").renameTo(new File("truststore.jks"));

    }

}
