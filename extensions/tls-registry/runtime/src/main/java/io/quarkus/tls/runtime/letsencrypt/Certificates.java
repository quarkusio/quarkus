package io.quarkus.tls.runtime.letsencrypt;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Creates the keys, the certificate signing request and the temporary self-signed certificate of the Let's Encrypt
 * client, and reads and writes them in PEM format. Only the JDK is used.
 */
final class Certificates {

    private static final String OID_EC_WITH_SHA256 = "1.2.840.10045.4.3.2";
    private static final String OID_COMMON_NAME = "2.5.4.3";
    private static final String OID_EXTENSION_REQUEST = "1.2.840.113549.1.9.14";
    private static final String OID_SUBJECT_ALT_NAME = "2.5.29.17";
    private static final String OID_AUTHORITY_KEY_IDENTIFIER = "2.5.29.35";
    private static final int DNS_NAME = 2;
    private static final int KEY_IDENTIFIER = 0;

    private Certificates() {
    }

    /**
     * Generate an EC key pair on the P-256 curve.
     *
     * @return the key pair
     * @throws GeneralSecurityException if the JDK does not support the curve
     */
    static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    /**
     * Create a PKCS#10 certificate signing request for the given domains, signed with the given key.
     *
     * @param keyPair the key pair of the certificate
     * @param domains the domains; the first one is also the common name
     * @return the DER encoding of the request
     * @throws GeneralSecurityException if the signature fails
     */
    static byte[] certificateSigningRequest(KeyPair keyPair, List<String> domains) throws GeneralSecurityException {
        byte[] extensionRequest = Der.sequence(
                Der.oid(OID_EXTENSION_REQUEST),
                Der.set(Der.sequence(subjectAlternativeName(domains))));
        byte[] info = Der.sequence(
                Der.integer(BigInteger.ZERO),
                name(domains.get(0)),
                keyPair.getPublic().getEncoded(),
                Der.explicit(0, extensionRequest));
        return signed(info, keyPair.getPrivate());
    }

    /**
     * Create a self-signed certificate for the given domains. The application uses it until Let's Encrypt
     * issues the first certificate.
     *
     * @param keyPair the key pair of the certificate
     * @param domains the domains; the first one is also the common name
     * @param validity the validity of the certificate from now
     * @return the certificate
     * @throws GeneralSecurityException if the signature fails
     */
    static X509Certificate selfSigned(KeyPair keyPair, List<String> domains, Duration validity)
            throws GeneralSecurityException {
        byte[] serial = new byte[16];
        new SecureRandom().nextBytes(serial);
        ZonedDateTime now = ZonedDateTime.now();
        byte[] name = name(domains.get(0));
        byte[] tbs = Der.sequence(
                Der.explicit(0, Der.integer(BigInteger.TWO)),
                Der.integer(new BigInteger(1, serial)),
                Der.sequence(Der.oid(OID_EC_WITH_SHA256)),
                name,
                Der.sequence(Der.time(now.minusMinutes(5)), Der.time(now.plus(validity))),
                name,
                keyPair.getPublic().getEncoded(),
                Der.explicit(3, Der.sequence(subjectAlternativeName(domains))));
        byte[] encoded = signed(tbs, keyPair.getPrivate());
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(encoded));
    }

    /**
     * Compute the certificate identifier of ACME Renewal Information (RFC 9773): the base64url encoding of the
     * authority key identifier, a dot, and the base64url encoding of the serial number.
     *
     * @param certificate the certificate
     * @return the identifier, or {@code null} if the certificate has no authority key identifier
     */
    static String renewalCertificateId(X509Certificate certificate) {
        byte[] extension = certificate.getExtensionValue(OID_AUTHORITY_KEY_IDENTIFIER);
        if (extension == null) {
            return null;
        }
        Der.Element octets = Der.read(extension, 0);
        Der.Element sequence = Der.read(octets.input(), octets.contentStart());
        byte[] keyIdentifier = null;
        for (Der.Element child = sequence.firstChild(); child != null; child = child.next(sequence.contentEnd())) {
            if (child.tag() == (0x80 | KEY_IDENTIFIER)) {
                keyIdentifier = child.content();
                break;
            }
        }
        if (keyIdentifier == null) {
            return null;
        }
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(keyIdentifier) + "."
                + encoder.encodeToString(certificate.getSerialNumber().toByteArray());
    }

    /**
     * Encode a private key in PKCS#8 PEM format.
     *
     * @param key the key
     * @return the PEM text
     */
    static String privateKeyPem(PrivateKey key) {
        return pem("PRIVATE KEY", key.getEncoded());
    }

    /**
     * Encode certificates in PEM format.
     *
     * @param chain the certificates, the end-entity certificate first
     * @return the PEM text
     * @throws GeneralSecurityException if a certificate cannot be encoded
     */
    static String certificatesPem(List<X509Certificate> chain) throws GeneralSecurityException {
        StringBuilder builder = new StringBuilder();
        for (X509Certificate certificate : chain) {
            builder.append(pem("CERTIFICATE", certificate.getEncoded()));
        }
        return builder.toString();
    }

    /**
     * Encode a key pair in PEM format: the private key in PKCS#8, then the public key. The JDK cannot derive an EC
     * public key from its private key, so both are stored.
     *
     * @param keyPair the key pair
     * @return the PEM text
     */
    static String keyPairPem(KeyPair keyPair) {
        return privateKeyPem(keyPair.getPrivate()) + pem("PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    /**
     * Read an EC key pair written by {@link #keyPairPem(KeyPair)}.
     *
     * @param pem the PEM text
     * @return the key pair
     * @throws GeneralSecurityException if the text does not hold both keys
     */
    static KeyPair readKeyPair(String pem) throws GeneralSecurityException {
        PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(
                new PKCS8EncodedKeySpec(block(pem, "PRIVATE KEY")));
        PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(
                new X509EncodedKeySpec(block(pem, "PUBLIC KEY")));
        return new KeyPair(publicKey, privateKey);
    }

    /**
     * Read an EC private key in PKCS#8 PEM format.
     *
     * @param pem the PEM text
     * @return the key
     * @throws GeneralSecurityException if the text is not an EC private key
     */
    static PrivateKey readPrivateKey(String pem) throws GeneralSecurityException {
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(block(pem, "PRIVATE KEY")));
    }

    /**
     * Read the certificates of a PEM chain.
     *
     * @param pem the PEM text
     * @return the certificates in the order of the text
     * @throws GeneralSecurityException if the text has no valid certificate
     */
    static List<X509Certificate> readCertificates(String pem) throws GeneralSecurityException {
        List<X509Certificate> result = new ArrayList<>();
        for (var certificate : CertificateFactory.getInstance("X.509")
                .generateCertificates(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)))) {
            result.add((X509Certificate) certificate);
        }
        if (result.isEmpty()) {
            throw new GeneralSecurityException("No certificate found");
        }
        return result;
    }

    private static byte[] name(String commonName) {
        return Der.sequence(Der.set(Der.sequence(Der.oid(OID_COMMON_NAME), Der.utf8String(commonName))));
    }

    private static byte[] subjectAlternativeName(List<String> domains) {
        byte[][] names = new byte[domains.size()][];
        for (int i = 0; i < names.length; i++) {
            names[i] = Der.implicitPrimitive(DNS_NAME, domains.get(i).getBytes(StandardCharsets.US_ASCII));
        }
        return Der.sequence(Der.oid(OID_SUBJECT_ALT_NAME), Der.octetString(Der.sequence(names)));
    }

    private static byte[] signed(byte[] content, PrivateKey key) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(key);
        signature.update(content);
        return Der.sequence(content, Der.sequence(Der.oid(OID_EC_WITH_SHA256)), Der.bitString(signature.sign()));
    }

    private static byte[] block(String pem, String type) throws GeneralSecurityException {
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        int start = pem.indexOf(begin);
        int stop = pem.indexOf(end);
        if (start < 0 || stop < start) {
            throw new GeneralSecurityException("No " + type + " PEM block found");
        }
        return Base64.getMimeDecoder().decode(pem.substring(start + begin.length(), stop));
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }
}
