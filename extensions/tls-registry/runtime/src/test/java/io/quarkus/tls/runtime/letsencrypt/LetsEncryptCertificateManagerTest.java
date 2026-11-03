package io.quarkus.tls.runtime.letsencrypt;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class LetsEncryptCertificateManagerTest {

    @Test
    void renewsWhenAThirdOfTheValidityRemains() throws Exception {
        X509Certificate certificate = Certificates.selfSigned(Certificates.generateKeyPair(), List.of("example.org"),
                Duration.ofDays(90));

        Instant renewal = LetsEncryptCertificateManager.fallbackRenewalTime(certificate);

        Duration lifetime = Duration.between(certificate.getNotBefore().toInstant(),
                certificate.getNotAfter().toInstant());
        assertThat(renewal).isEqualTo(certificate.getNotAfter().toInstant().minus(lifetime.dividedBy(3)));
    }

    @Test
    void renewsShortLivedCertificatesAtHalfOfTheirValidity() throws Exception {
        X509Certificate certificate = Certificates.selfSigned(Certificates.generateKeyPair(), List.of("example.org"),
                Duration.ofDays(6));

        Instant renewal = LetsEncryptCertificateManager.fallbackRenewalTime(certificate);

        Duration lifetime = Duration.between(certificate.getNotBefore().toInstant(),
                certificate.getNotAfter().toInstant());
        assertThat(renewal).isEqualTo(certificate.getNotAfter().toInstant().minus(lifetime.dividedBy(2)));
    }
}
