package io.quarkus.security.webauthn.test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Generates the certificates used to test the attestation trust anchors: a CA, an authenticator attestation
 * certificate issued by that CA, and a self-signed authenticator attestation certificate which no CA vouches for.
 * <p>
 * {@code QuarkusExtensionTest} initializes the test class a second time in the application class loader, and both
 * instances must agree on the certificates, so they are written once to {@link #DIRECTORY}, and reused if they are
 * already there.
 */
final class AttestationCertificates {

    static final Path DIRECTORY = Path.of("target", "webauthn-attestation");

    static final Path CA = DIRECTORY.resolve("ca.pem");

    private static final Path TRUSTED = DIRECTORY.resolve("authenticator.pem");
    private static final Path TRUSTED_KEY = DIRECTORY.resolve("authenticator-key.pem");
    private static final Path UNTRUSTED = DIRECTORY.resolve("untrusted-authenticator.pem");
    private static final Path UNTRUSTED_KEY = DIRECTORY.resolve("untrusted-authenticator-key.pem");

    private AttestationCertificates() {
    }

    /**
     * @return the path of the CA, with forward slashes so that it survives a properties file on Windows
     */
    static String caPath() {
        return CA.toString().replace('\\', '/');
    }

    /**
     * Writes the certificates if they don't exist yet.
     */
    static synchronized void generate() {
        try {
            if (Files.exists(UNTRUSTED_KEY)) {
                // written last
                return;
            }
            Files.createDirectories(DIRECTORY);
            KeyPair caKeys = newKeyPair();
            X500Name caName = new X500Name("C=US, O=Quarkus, CN=Quarkus WebAuthn Test Attestation CA");
            X509Certificate ca = certificate(caName, caKeys, caName, caKeys.getPrivate(), true);

            X500Name authenticatorName = authenticatorName("Quarkus WebAuthn Test Authenticator");
            KeyPair authenticatorKeys = newKeyPair();
            X509Certificate authenticator = certificate(authenticatorName, authenticatorKeys, caName,
                    caKeys.getPrivate(), false);

            X500Name untrustedName = authenticatorName("Quarkus WebAuthn Untrusted Authenticator");
            KeyPair untrustedKeys = newKeyPair();
            X509Certificate untrusted = certificate(untrustedName, untrustedKeys, untrustedName,
                    untrustedKeys.getPrivate(), false);

            write(CA, "CERTIFICATE", ca.getEncoded());
            write(TRUSTED, "CERTIFICATE", authenticator.getEncoded());
            write(TRUSTED_KEY, "PRIVATE KEY", authenticatorKeys.getPrivate().getEncoded());
            write(UNTRUSTED, "CERTIFICATE", untrusted.getEncoded());
            write(UNTRUSTED_KEY, "PRIVATE KEY", untrustedKeys.getPrivate().getEncoded());
        } catch (IOException | GeneralSecurityException | OperatorCreationException e) {
            throw new RuntimeException(e);
        }
    }

    static X509Certificate trustedCertificate() {
        return readCertificate(TRUSTED);
    }

    static PrivateKey trustedKey() {
        return readPrivateKey(TRUSTED_KEY);
    }

    static X509Certificate untrustedCertificate() {
        return readCertificate(UNTRUSTED);
    }

    static PrivateKey untrustedKey() {
        return readPrivateKey(UNTRUSTED_KEY);
    }

    private static X500Name authenticatorName(String commonName) {
        // webauthn4j requires the OU of a packed attestation certificate to be "Authenticator Attestation"
        return new X500Name("C=US, O=Quarkus, OU=Authenticator Attestation, CN=" + commonName);
    }

    private static KeyPair newKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static X509Certificate certificate(X500Name subject, KeyPair subjectKeys, X500Name issuer,
            PrivateKey issuerKey, boolean ca) throws GeneralSecurityException, OperatorCreationException, IOException {
        Instant now = Instant.now();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer,
                new BigInteger(64, new SecureRandom()), Date.from(now.minus(Duration.ofDays(1))),
                Date.from(now.plus(Duration.ofDays(3650))), subject, subjectKeys.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(issuerKey);
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    private static void write(Path path, String type, byte[] der) throws IOException {
        String pem = "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der)
                + "\n-----END " + type + "-----\n";
        Files.writeString(path, pem, StandardCharsets.US_ASCII);
    }

    private static X509Certificate readCertificate(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static PrivateKey readPrivateKey(Path path) {
        try {
            String pem = Files.readString(path, StandardCharsets.US_ASCII);
            byte[] der = Base64.getMimeDecoder().decode(pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", ""));
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
