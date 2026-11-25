package io.quarkus.tls.runtime.letsencrypt;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class CertificatesTest {

    @Test
    void certificateSigningRequestIsSignedAndNamesTheDomains() throws Exception {
        KeyPair keyPair = Certificates.generateKeyPair();

        byte[] csr = Certificates.certificateSigningRequest(keyPair, List.of("example.org", "www.example.org"));

        Der.Element request = Der.read(csr, 0);
        assertThat(request.contentEnd()).isEqualTo(csr.length);
        Der.Element info = request.firstChild();
        Der.Element algorithm = info.next(request.contentEnd());
        Der.Element signatureBits = algorithm.next(request.contentEnd());

        Der.Element version = info.firstChild();
        Der.Element subject = version.next(info.contentEnd());
        Der.Element publicKeyInfo = subject.next(info.contentEnd());
        Der.Element attributes = publicKeyInfo.next(info.contentEnd());
        assertThat(version.content()).containsExactly(0);
        assertThat(attributes.tag()).isEqualTo(0xA0);

        byte[] encodedKey = slice(csr, publicKeyInfo);
        PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encodedKey));
        assertThat(publicKey).isEqualTo(keyPair.getPublic());

        byte[] signatureValue = signatureBits.content();
        assertThat(signatureValue[0]).isZero();
        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(publicKey);
        verifier.update(slice(csr, info));
        assertThat(verifier.verify(Arrays.copyOfRange(signatureValue, 1, signatureValue.length))).isTrue();

        assertThat(dnsNames(attributes)).containsExactly("example.org", "www.example.org");
    }

    @Test
    void selfSignedCertificateCoversTheDomains() throws Exception {
        KeyPair keyPair = Certificates.generateKeyPair();

        X509Certificate certificate = Certificates.selfSigned(keyPair, List.of("example.org", "api.example.org"),
                Duration.ofDays(7));

        certificate.verify(keyPair.getPublic());
        certificate.checkValidity();
        assertThat(certificate.getVersion()).isEqualTo(3);
        assertThat(certificate.getSubjectX500Principal().getName()).isEqualTo("CN=example.org");
        assertThat(certificate.getSubjectAlternativeNames())
                .extracting(name -> (Object) name.get(1))
                .containsExactly("example.org", "api.example.org");
        assertThat(certificate.getNotAfter()).isBefore(Date.from(Instant.now().plus(Duration.ofDays(8))));
    }

    @Test
    void pemRoundTrip() throws Exception {
        KeyPair keyPair = Certificates.generateKeyPair();
        X509Certificate certificate = Certificates.selfSigned(keyPair, List.of("example.org"), Duration.ofDays(1));

        String keyPem = Certificates.privateKeyPem(keyPair.getPrivate());
        String chainPem = Certificates.certificatesPem(List.of(certificate, certificate));

        assertThat(keyPem).startsWith("-----BEGIN PRIVATE KEY-----\n").endsWith("-----END PRIVATE KEY-----\n");
        assertThat(Certificates.readPrivateKey(keyPem)).isEqualTo(keyPair.getPrivate());
        assertThat(Certificates.readCertificates(chainPem)).containsExactly(certificate, certificate);
    }

    @Test
    void renewalCertificateIdNeedsAnAuthorityKeyIdentifier() throws Exception {
        KeyPair keyPair = Certificates.generateKeyPair();
        X509Certificate certificate = Certificates.selfSigned(keyPair, List.of("example.org"), Duration.ofDays(1));

        assertThat(Certificates.renewalCertificateId(certificate)).isNull();
    }

    private static byte[] slice(byte[] input, Der.Element element) {
        int start = element.contentStart() - headerLength(input, element);
        return Arrays.copyOfRange(input, start, element.contentEnd());
    }

    private static int headerLength(byte[] input, Der.Element element) {
        int length = element.contentEnd() - element.contentStart();
        if (length < 0x80) {
            return 2;
        }
        return 2 + (Integer.SIZE - Integer.numberOfLeadingZeros(length) + 7) / 8;
    }

    private static List<String> dnsNames(Der.Element attributes) {
        // [0] { Attribute { extensionRequest, SET { Extensions { Extension { subjectAltName, OCTET STRING } } } } }
        Der.Element attribute = attributes.firstChild();
        Der.Element values = attribute.firstChild().next(attribute.contentEnd());
        Der.Element extensions = values.firstChild();
        Der.Element extension = extensions.firstChild();
        Der.Element value = extension.firstChild().next(extension.contentEnd());
        Der.Element generalNames = Der.read(value.input(), value.contentStart());
        List<String> names = new ArrayList<>();
        for (Der.Element name = generalNames.firstChild(); name != null; name = name.next(generalNames.contentEnd())) {
            assertThat(name.tag()).isEqualTo(0x82);
            names.add(new String(name.content(), StandardCharsets.US_ASCII));
        }
        return names;
    }
}
